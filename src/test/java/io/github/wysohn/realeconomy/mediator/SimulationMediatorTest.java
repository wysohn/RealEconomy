package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.TransactionUtil;
import io.github.wysohn.realeconomy.manager.banking.account.TradingAccount;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.simulation.Agent;
import io.github.wysohn.realeconomy.manager.simulation.MarketSimulationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class SimulationMediatorTest {
    public static final ItemStackSignature WHEAT = new ItemStackSignature(Material.WHEAT);
    public static final ItemStackSignature COCOA = new ItemStackSignature(Material.COCOA_BEANS);
    public static final ItemStackSignature COOKIE = new ItemStackSignature(Material.COOKIE);
    public static final ItemStackSignature BREAD = new ItemStackSignature(Material.BREAD);

    private AssetListingManager assetListingManager;
    private MarketSimulationManager marketSimulationManager;
    private Logger logger;
    private TradeMediator tradeMediator;
    private BankingMediator bankingMediator;
    private CentralBank centralBank;
    private Currency currency;
    private Agent agent1;
    private Agent agent2;

    @Before
    public void init() throws Exception {
        Server server = mock(Server.class);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        assetListingManager = mock(AssetListingManager.class);
        marketSimulationManager = mock(MarketSimulationManager.class);
        logger = mock(Logger.class);
        tradeMediator = mock(TradeMediator.class);
        bankingMediator = mock(BankingMediator.class);

        centralBank = mock(CentralBank.class);
        currency = mock(Currency.class);

        when(centralBank.getBaseCurrency()).thenReturn(currency);
        when(currency.ownerBank()).thenReturn(centralBank);

        Map<UUID, Agent> agentMap = new HashMap<>();
        agent1 = new Agent(logger,
                UUID.randomUUID(),
                "Pastry_1",
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(WHEAT, 200.0));
                    add(Pair.of(COCOA, 100.0));
                }},
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(COOKIE, 800.0));
                }});
        agent2 = new Agent(logger,
                UUID.randomUUID(),
                "Pastry_2",
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(WHEAT, 300.0));
                }},
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(BREAD, 100.0));
                }});
        agentMap.put(agent1.getUuid(), agent1);
        agentMap.put(agent2.getUuid(), agent2);

        when(marketSimulationManager.getAgents()).thenReturn(agentMap.values());
    }

    @Test
    public void testSimulatorBid() throws Exception{
        when(bankingMediator.send(any(IBankUser.class),
                any(IBankingType.class),
                any(),
                any(),
                any())).thenReturn(TransactionUtil.Result.OK);
        when(bankingMediator.send(any(),
                any(IBankUser.class),
                any(IBankingType.class),
                any(),
                any())).thenReturn(TransactionUtil.Result.OK);

        SimulationMediator.MarketSimulator simulator = new SimulationMediator.MarketSimulator(
                assetListingManager,
                logger,
                marketSimulationManager,
                tradeMediator,
                centralBank,
                bankingMediator);

        simulator.iterate();

        verify(assetListingManager).addOrder(eq(WHEAT),
                eq(OrderType.BUY),
                eq(agent1),
                eq(1.01),
                eq(currency),
                eq(200));
        assertEquals(BigDecimal.valueOf(1.01),
                agent1.getCurrentPricing(WHEAT));

        verify(assetListingManager).addOrder(eq(COCOA),
                eq(OrderType.BUY),
                eq(agent1),
                eq(1.01),
                eq(currency),
                eq(100));
        assertEquals(BigDecimal.valueOf(1.01),
                agent1.getCurrentPricing(COCOA));

        verify(assetListingManager).addOrder(eq(WHEAT),
                eq(OrderType.BUY),
                eq(agent2),
                eq(1.01),
                eq(currency),
                eq(300));
        assertEquals(BigDecimal.valueOf(1.01),
                agent2.getCurrentPricing(WHEAT));

        // an hour later
        simulator.iterate();

        verify(assetListingManager).addOrder(eq(WHEAT),
                eq(OrderType.BUY),
                eq(agent1),
                eq(1.0201),
                eq(currency),
                eq(200));
        assertEquals(BigDecimal.valueOf(1.0201),
                agent1.getCurrentPricing(WHEAT));

        verify(assetListingManager).addOrder(eq(COCOA),
                eq(OrderType.BUY),
                eq(agent1),
                eq(1.0201),
                eq(currency),
                eq(100));
        assertEquals(BigDecimal.valueOf(1.0201),
                agent1.getCurrentPricing(COCOA));

        verify(assetListingManager).addOrder(eq(WHEAT),
                eq(OrderType.BUY),
                eq(agent2),
                eq(1.0201),
                eq(currency),
                eq(300));
        assertEquals(BigDecimal.valueOf(1.0201),
                agent2.getCurrentPricing(WHEAT));
    }

    @Test
    public void testSimulatorWithdraw() throws Exception{
        SimulationMediator.MarketSimulator simulator = new SimulationMediator.MarketSimulator(
                assetListingManager,
                logger,
                marketSimulationManager,
                tradeMediator,
                centralBank,
                bankingMediator);
        Map<IBankUser, TradingAccount> accountMap = new HashMap<>();

        when(centralBank.removeAccountAsset(any(), any(), anyInt()))
                .then(invocation -> {
                    IBankUser agent = (IBankUser) invocation.getArguments()[0];
                    AssetSignature sign = (AssetSignature) invocation.getArguments()[1];
                    int amount = (int) invocation.getArguments()[2];

                    return accountMap.computeIfAbsent(agent, (key -> new TradingAccount()))
                            .removeAsset(sign, amount);
                });
        doAnswer(invocation -> {
            IBankUser user = (IBankUser) invocation.getArguments()[0];
            Asset asset = (Asset) invocation.getArguments()[1];

            accountMap.computeIfAbsent(user, (key -> new TradingAccount()))
                    .addAsset(asset);

            return null;
        }).when(centralBank).addAccountAsset(any(), any());
        when(centralBank.countAccountAsset(any(), any())).then(invocation -> {
            IBankUser user = (IBankUser) invocation.getArguments()[0];
            AssetSignature sign = (AssetSignature) invocation.getArguments()[1];

            return accountMap.computeIfAbsent(user, (key -> new TradingAccount()))
                    .countAsset(sign);
        });

        // no resources
        simulator.iterate();

        assertFalse(agent1.canProduce());
        assertFalse(agent2.canProduce());

        // some resources arrived at bank by bidding
        accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .addAsset(WHEAT.asset(400.0));
        accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .addAsset(COCOA.asset(200.0));

        accountMap.computeIfAbsent(agent2, (key -> new TradingAccount()))
                .addAsset(WHEAT.asset(600.0));

        // transfer from bank account -> agent
        // 200 wheat 100 cocoa
        // generate 100 cookies
        simulator.iterate();
        assertEquals(400.0 - 200.0, accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .countAsset(WHEAT), 0.00001);
        assertEquals(200.0 - 100.0, accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .countAsset(COCOA), 0.00001);
        assertEquals(0.0 + 800.0, accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .countAsset(COOKIE), 0.00001);

        assertEquals(600.0 - 300.0, accountMap.computeIfAbsent(agent2, (key -> new TradingAccount()))
                .countAsset(WHEAT), 0.00001);
        assertEquals(0.0 + 100.0, accountMap.computeIfAbsent(agent2, (key -> new TradingAccount()))
                .countAsset(BREAD), 0.00001);

        // transfer from bank account -> agent
        // 200 wheat 100 cocoa
        // generate 100 cookies
        simulator.iterate();
        assertEquals(200.0 - 200.0, accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .countAsset(WHEAT), 0.00001);
        assertEquals(100.0 - 100.0, accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .countAsset(COCOA), 0.00001);
        assertEquals(800.0 + 800.0, accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .countAsset(COOKIE), 0.00001);

        assertEquals(300.0 - 300.0, accountMap.computeIfAbsent(agent2, (key -> new TradingAccount()))
                .countAsset(WHEAT), 0.00001);
        assertEquals(100.0 + 100.0, accountMap.computeIfAbsent(agent2, (key -> new TradingAccount()))
                .countAsset(BREAD), 0.00001);

        // no more resources
        simulator.iterate();

        assertEquals(0.0, accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .countAsset(WHEAT), 0.00001);
        assertEquals(0.0, accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .countAsset(COCOA), 0.00001);
        assertEquals(1600.0, accountMap.computeIfAbsent(agent1, (key -> new TradingAccount()))
                .countAsset(COOKIE), 0.00001);

        assertEquals(0.0, accountMap.computeIfAbsent(agent2, (key -> new TradingAccount()))
                .countAsset(WHEAT), 0.00001);
        assertEquals(200.0, accountMap.computeIfAbsent(agent2, (key -> new TradingAccount()))
                .countAsset(BREAD), 0.00001);
    }
}