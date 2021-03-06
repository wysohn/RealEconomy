package io.github.wysohn.realeconomy.manager.listing;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.plugin.ITaskSupervisor;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.rapidframework3.utils.trie.StringListTrie;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.trade.IOrderQueryModule;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * id(auto increment)
 * register_date
 * current_stock(int)
 * max_stock(int)
 * signature_type(simple class name)
 * signature_data(json)
 */
@Singleton
public class AssetListingManager extends AbstractManagerElementCaching<UUID, AssetListing> {
    private final IOrderQueryModule orderQueryModule;
    private final Map<AssetSignature, UUID> signatureUUIDMap = new HashMap<>();
    private final ITaskSupervisor taskSupervisor;

    @Inject
    public AssetListingManager(@Named("pluginName") String pluginName,
                               @PluginLogger Logger logger,
                               ManagerConfig config,
                               @PluginDirectory File pluginDir,
                               IShutdownHandle shutdownHandle,
                               ISerializer serializer,
                               ITypeAsserter asserter,
                               Injector injector,
                               IOrderQueryModule orderQueryModule,
                               ITaskSupervisor taskSupervisor) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, asserter, injector, AssetListing.class);
        this.orderQueryModule = orderQueryModule;
        this.taskSupervisor = taskSupervisor;
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory("assetListings");
    }

    @Override
    protected UUID fromString(String s) {
        return UUID.fromString(s);
    }

    @Override
    protected AssetListing newInstance(UUID uuid) {
        return new AssetListing(uuid);
    }

    @Override
    public void enable() throws Exception {
        super.enable();

        orderQueryModule.clearTemporaryBuyOrders();
        orderQueryModule.clearTemporarySellOrders();

        forEach(listing -> {
            signatureUUIDMap.put(listing.getSignature(), listing.getKey());
            orderQueryModule.setListingName(listing.getKey(), listing.getSignature().toString());
            try {
                orderQueryModule.commitOrders();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void disable() throws Exception {
        super.disable();

        orderQueryModule.clearTemporaryBuyOrders();
        orderQueryModule.clearTemporarySellOrders();
    }

    /**
     * @param key
     * @return
     * @deprecated use {@link #newListing(AssetSignature)}
     */
    @Override
    public Optional<WeakReference<AssetListing>> getOrNew(UUID key) {
        return super.getOrNew(key);
    }

    /**
     * List the given signature to the market to be visible.
     *
     * @param signature
     * @return
     */
    public boolean newListing(AssetSignature signature) {
        if (signatureUUIDMap.containsKey(signature))
            return false;

        super.getOrNew(UUID.randomUUID())
                .map(Reference::get)
                .ifPresent(listing -> {
                    listing.setSignature(signature);
                    signatureUUIDMap.put(signature, listing.getKey());

                    // process blocking operation asynchronously
                    taskSupervisor.async(() -> {
                        orderQueryModule.setListingName(listing.getKey(), listing.getSignature().toString());
                        try {
                            orderQueryModule.commitOrders();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });
                });

        return true;
    }

    /**
     * Create a new category to be used. Invoke it when config file is reloaded, so the new category
     * names can be registered.
     * <p>
     * This is always successful as it assigns new id for the category that did not exist.
     *
     * @param category category name
     */
    public void newCategory(String category) {
        orderQueryModule.getCategoryId(category);
    }

    public UUID signatureToUuid(AssetSignature sign) {
        newListing(sign);
        return signatureUUIDMap.get(sign);
    }

    /**
     * Get listing correspond to the signature. For example, if it's an ItemStackSignature,
     * we expect similar ItemStacks to be show up as one instead of all of them having different
     * instance.
     *
     * @param signature
     * @return
     */
    public AssetListing fromSignature(AssetSignature signature) {
        return Optional.ofNullable(signature)
                .map(signatureUUIDMap::get)
                .flatMap(this::get)
                .map(Reference::get)
                .orElse(null);
    }

    /**
     * Add order for the given signature. The given signature must be listed already using
     * {@link #newListing(AssetSignature)}
     * <p>
     * this must be finalized using {@link #commitOrders()}
     * <p>
     * This is a thread-safe operation, yet it will be blocked while trade matching is under progress.
     * Typically, the search process is around 100ms, so this method is better not called in server thread.
     *
     * @param signature asset signature
     * @param type      trade type
     * @param issuer    issuer
     * @param price     price
     * @param currency  currency of price
     * @param stock     amount to purchase/sell
     * @param temp      mark this order as temporary. Will be deleted when plugin disables.
     */
    public void addOrder(AssetSignature signature,
                         OrderType type,
                         IOrderIssuer issuer,
                         double price,
                         Currency currency,
                         int stock,
                         boolean temp) throws SQLException {
        Validation.assertNotNull(type);
        Validation.assertNotNull(issuer);
        Validation.validate(price, val -> val > 0.0, "Price cannot be 0 or less.");
        Validation.assertNotNull(currency);
        Validation.validate(stock, val -> val > 0, "Stock cannot be 0 or less.");

        if (!signatureUUIDMap.containsKey(signature))
            throw new RuntimeException("Invalid signature.");
        AssetListing listing = fromSignature(signature);

        orderQueryModule.addOrder(listing.getKey(),
                signature.category(),
                type,
                issuer,
                price,
                currency,
                stock,
                temp);
    }

    /**
     * {@link #addOrder(AssetSignature, OrderType, IOrderIssuer, double, Currency, int, boolean)}
     *
     * @param signature
     * @param type
     * @param issuer
     * @param price
     * @param currency
     * @param stock
     * @throws SQLException
     */
    public void addOrder(AssetSignature signature,
                         OrderType type,
                         IOrderIssuer issuer,
                         double price,
                         Currency currency,
                         int stock) throws SQLException {
        addOrder(signature, type, issuer, price, currency, stock, false);
    }

    public OrderInfo getInfo(int orderId, OrderType type) throws SQLException {
        Validation.validate(orderId, val -> val > 0, "orderId must be larger than 0.");
        Validation.assertNotNull(type);

        return orderQueryModule.getInfo(orderId, type);
    }

    public void editOrder(int orderId, OrderType type, int newAmount) throws SQLException {
        Validation.validate(orderId, val -> val > 0, "orderId must be larger than 0.");
        Validation.assertNotNull(type);
        Validation.validate(newAmount, val -> val > 0, "amount must be larger than 0. Maybe delete instead?");

        orderQueryModule.editOrder(orderId,
                type,
                newAmount);
    }

    /**
     * Cancel the previously submitted order. This is valid until the trade is not yet finalized.
     * <p>
     * This is a thread-safe operation, yet it will be blocked while trade matching is under progress.
     * Typically, the search process is around 100ms, so this method is better not called in server thread.
     * <p>
     * this must be finalized using {@link #commitOrders()}
     *
     * @param orderId  the order id to cancel
     * @param type     the order type
     * @param callback callback function. Provides same id provided in 'orderId' upon successful cancellation,
     *                 or 0 if failed.
     * @throws SQLException something went wrong with SQL operation.
     */
    public void cancelOrder(int orderId, OrderType type, Consumer<Integer> callback) throws SQLException {
        Validation.validate(orderId, val -> val > 0, "orderId must be larger than 0.");
        Validation.assertNotNull(type);

        orderQueryModule.cancelOrder(orderId,
                type,
                callback);
    }

    /**
     * Log the given trade info.
     * <p>
     * This is a thread-safe operation, yet it will be blocked while trade matching is under progress.
     * Typically, the search process is around 100ms, so this method is better not called in server thread.
     * <p>
     * this must be finalized using {@link #commitOrders()}
     *
     * @param info   the info used in the trade
     * @param amount the amounts actually traded; this may differ from the TradeInfo's amount since buyer/seller is
     *               not obligated to buy/sell the listed amount. Trade occurs only when both ends agree with each
     *               other.
     * @throws SQLException something went wrong with SQL operation.
     */
    public void logOrder(TradeInfo info, int amount) throws SQLException {
        Validation.assertNotNull(info);
        Validation.validate(amount, val -> val > 0, "amount must be larger than 0.");

        orderQueryModule.logOrder(info.getListingUuid(),
                info.getCategoryId(),
                info.getSeller(),
                info.getBuyer(),
                info.getAsk(),
                info.getCurrencyUuid(),
                amount);
    }

    public void commitOrders() throws SQLException {
        orderQueryModule.commitOrders();
    }

    public void rollbackOrders() throws SQLException {
        orderQueryModule.rollbackOrders();
    }

    public PricePoint getLastPrice(AssetSignature sign, Currency currency) {
        newListing(sign);
        UUID uuid = signatureUUIDMap.get(sign);

        return orderQueryModule.getLastTradingPrice(7, currency.getKey(), uuid);
    }

    public double getAveragePrice(AssetSignature sign, Currency currency) {
        newListing(sign);
        UUID uuid = signatureUUIDMap.get(sign);

        return orderQueryModule.getLastTradingAverage(7, currency.getKey(), uuid);
    }

    public OrderInfo getLowestAsk(AssetSignature sign, Currency currency) {
        newListing(sign);
        UUID uuid = signatureUUIDMap.get(sign);

        return orderQueryModule.getLowestAsk(currency.getKey(), uuid);
    }

    public OrderInfo getHighestBid(AssetSignature sign, Currency currency) {
        newListing(sign);
        UUID uuid = signatureUUIDMap.get(sign);

        return orderQueryModule.getHighestBid(currency.getKey(), uuid);
    }

    public PricePoint getHighestPrice(AssetSignature sign, Currency currency) {
        newListing(sign);
        UUID uuid = signatureUUIDMap.get(sign);

        return orderQueryModule.getHighestPoint(7, currency.getKey(), uuid);
    }

    public PricePoint getLowestPrice(AssetSignature sign, Currency currency) {
        newListing(sign);
        UUID uuid = signatureUUIDMap.get(sign);

        return orderQueryModule.getLowestPoint(7, currency.getKey(), uuid);
    }

    /**
     * Peek one best pair of buy/sell order which can be traded.
     * <p>
     * This is a thread-safe operation, yet it's a resource intensive operation, so it's better to not call
     * this method in server thread.
     *
     * @param consumer
     */
    public void peekMatchingOrder(Consumer<TradeInfo> consumer) {
        orderQueryModule.peekMatchingOrders(consumer);
    }

    public Collection<String> categoryNames() {
        return orderQueryModule.categoryNames();
    }

    public StringListTrie getCategoryTrie() {
        return orderQueryModule.categoryList();
    }

    /**
     * Get DataProvider of given asset signature.
     *
     * @param type     order type. For sell orders, lowest prices will be returned,
     *                 and for buy orders, highest prices will be returned.
     * @param category the category to search for. It can be null to search for all listings.
     * @return the DataProvider.
     * @throws RuntimeException if given category does not exist. Make sure to use only the values provided
     *                          by {@link #getCategoryTrie()}
     */
    public DataProvider<OrderInfo> getListedOrderProvider(OrderType type, String category) {
        return orderQueryModule.getListedOrderProvider(type, category);
    }
}
