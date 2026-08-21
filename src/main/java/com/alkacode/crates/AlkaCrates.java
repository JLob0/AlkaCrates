package com.alkacode.crates;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.plugin.AlkaPlugin;
import com.alkacode.crates.config.CratesConfig;
import com.alkacode.crates.config.MenuConfig;
import com.alkacode.crates.gui.layout.GuiLayoutLoader;
import com.alkacode.crates.animation.AnimationEngine;
import com.alkacode.crates.command.AlkaCratesCommand;
import com.alkacode.crates.command.CrateCommand;
import com.alkacode.crates.crate.placement.PlacedCrateManager;
import com.alkacode.crates.crate.service.CratePlacementService;
import com.alkacode.crates.crate.service.CrateService;
import com.alkacode.crates.crate.service.KeyService;
import com.alkacode.crates.crate.service.PityManager;
import com.alkacode.crates.crate.service.PityService;
import com.alkacode.crates.crate.service.RewardPityManager;
import com.alkacode.crates.crate.service.RewardWinManager;
import com.alkacode.crates.crate.service.VirtualKeyManager;
import com.alkacode.crates.engine.BetterModelSpawner;
import com.alkacode.crates.engine.CraftEngineSpawner;
import com.alkacode.crates.engine.CrateEngineType;
import com.alkacode.crates.engine.ItemsAdderFurnitureSpawner;
import com.alkacode.crates.engine.ModelBasedCrateEngine;
import com.alkacode.crates.engine.ModelEngineSpawner;
import com.alkacode.crates.hook.economy.AlkaEconomyHook;
import com.alkacode.crates.hook.item.AlkaItemsHook;
import com.alkacode.crates.hook.item.HeadDatabaseHook;
import com.alkacode.crates.hook.item.ItemHook;
import com.alkacode.crates.hook.item.ItemsAdderHook;
import com.alkacode.crates.hook.item.MMOItemsHook;
import com.alkacode.crates.hook.item.MythicItemsHook;
import com.alkacode.crates.hook.item.NexoHook;
import com.alkacode.crates.listener.CrateChatInputListener;
import com.alkacode.crates.listener.CrateInteractionListener;
import com.alkacode.crates.listener.CratePlayerDataListener;
import com.alkacode.crates.listener.CrateProtectionListener;
import com.alkacode.crates.menu.editor.ChatInputManager;
import com.alkacode.crates.menu.editor.CrateFileService;
import com.alkacode.crates.placeholder.CratesExpansion;
import com.alkacode.crates.repository.CrateLocationRepository;
import com.alkacode.crates.repository.CrateLogRepository;
import com.alkacode.crates.repository.PityRepository;
import com.alkacode.crates.repository.RewardPityRepository;
import com.alkacode.crates.repository.RewardWinRepository;
import com.alkacode.crates.repository.VirtualKeyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class AlkaCrates extends AlkaPlugin {

    private CratesConfig cratesConfig;
    private CratesMessages cratesMessages;
    private MenuConfig menuConfig;
    private GuiLayoutLoader guiLayoutLoader;
    private AnimationEngine animationEngine;
    private PlacedCrateManager placedCrateManager;
    private CratePlacementService placementService;
    private CrateService crateService;
    private KeyService keyService;
    private PityService pityService;
    private VirtualKeyRepository virtualKeyRepository;
    private CrateLocationRepository crateLocationRepository;
    private PityRepository pityRepository;
    private CrateLogRepository crateLogRepository;
    private RewardWinRepository rewardWinRepository;
    private RewardPityRepository rewardPityRepository;
    private VirtualKeyManager virtualKeyManager;
    private PityManager pityManager;
    private RewardWinManager rewardWinManager;
    private RewardPityManager rewardPityManager;
    private AlkaEconomyHook economyHook;
    private final List<ItemHook> itemHooks = new ArrayList<>();
    private CrateFileService crateFileService;
    private ChatInputManager chatInputManager;

    @Override
    protected void onPluginEnable() {
        saveDefaultConfig();
        AlkaAPI api = getAlkaAPI();

        this.cratesMessages = new CratesMessages(this);
        this.cratesConfig = new CratesConfig(this);
        cratesConfig.load();
        this.menuConfig = new MenuConfig(this);
        this.guiLayoutLoader = new GuiLayoutLoader(this);

        this.virtualKeyRepository = new VirtualKeyRepository(api.getDatabase());
        this.crateLocationRepository = new CrateLocationRepository(api.getDatabase());
        this.pityRepository = new PityRepository(api.getDatabase());
        this.crateLogRepository = new CrateLogRepository(api.getDatabase());
        this.rewardWinRepository = new RewardWinRepository(api.getDatabase());
        this.rewardPityRepository = new RewardPityRepository(api.getDatabase());
        try {
            virtualKeyRepository.createTable();
            crateLocationRepository.createTable();
            pityRepository.createTable();
            crateLogRepository.createTable();
            rewardWinRepository.createTable();
            rewardPityRepository.createTable();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Falha ao criar tabelas do AlkaCrates", e);
        }

        this.virtualKeyManager = new VirtualKeyManager(virtualKeyRepository, api.getScheduler(), getLogger());
        this.pityManager = new PityManager(pityRepository, api.getScheduler(), getLogger());
        this.rewardWinManager = new RewardWinManager(rewardWinRepository, api.getScheduler(), getLogger());
        this.rewardPityManager = new RewardPityManager(rewardPityRepository, api.getScheduler(), getLogger());
        rewardWinManager.loadGlobalOnEnable();
        for (org.bukkit.entity.Player online : getServer().getOnlinePlayers()) {
            virtualKeyManager.onJoin(online.getUniqueId());
            pityManager.onJoin(online.getUniqueId());
            rewardWinManager.onJoin(online.getUniqueId());
            rewardPityManager.onJoin(online.getUniqueId());
        }

        this.animationEngine = new AnimationEngine(this);
        this.placedCrateManager = new PlacedCrateManager();
        this.economyHook = new AlkaEconomyHook(getServer());
        registerItemHooks();

        this.placementService = new CratePlacementService(this);
        if (getServer().getPluginManager().isPluginEnabled("ModelEngine")) {
            placementService.registerEngine(CrateEngineType.MODELENGINE,
                    new ModelBasedCrateEngine(new ModelEngineSpawner(), CrateEngineType.MODELENGINE));
            getLogger().info("Engine de ModelEngine registrada.");
        }
        if (getServer().getPluginManager().isPluginEnabled("BetterModel")) {
            placementService.registerEngine(CrateEngineType.BETTERMODEL,
                    new ModelBasedCrateEngine(new BetterModelSpawner(), CrateEngineType.BETTERMODEL));
            getLogger().info("Engine de BetterModel registrada.");
        }
        if (getServer().getPluginManager().isPluginEnabled("CraftEngine")) {
            placementService.registerEngine(CrateEngineType.CRAFTENGINE,
                    new ModelBasedCrateEngine(new CraftEngineSpawner(), CrateEngineType.CRAFTENGINE));
            getLogger().info("Engine de CraftEngine registrada.");
        }
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            placementService.registerEngine(CrateEngineType.ITEMSADDER,
                    new ModelBasedCrateEngine(new ItemsAdderFurnitureSpawner(), CrateEngineType.ITEMSADDER));
            getLogger().info("Engine de ItemsAdder (furniture) registrada.");
        }
        this.crateService = new CrateService(this, rewardWinManager, rewardPityManager);
        this.keyService = new KeyService(this, virtualKeyManager);
        this.pityService = new PityService(this, pityManager);
        this.crateService.setPityService(pityService);
        this.placementService.loadAll();

        getCommand("crate").setExecutor(new CrateCommand(this));
        getCommand("crate").setTabCompleter(new CrateCommand(this));
        getCommand("alkacrates").setExecutor(new AlkaCratesCommand(this));
        getCommand("alkacrates").setTabCompleter(new AlkaCratesCommand(this));

        this.crateFileService = new CrateFileService(this);
        this.chatInputManager = new ChatInputManager();

        getServer().getPluginManager().registerEvents(new CrateInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(
                new CrateProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(
                new CratePlayerDataListener(virtualKeyManager, pityManager, rewardWinManager, rewardPityManager), this);
        getServer().getPluginManager().registerEvents(
                new CrateChatInputListener(this, chatInputManager), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CratesExpansion(this).register();
        }

        getServer().getServicesManager().register(com.alkacode.crates.api.AlkaCratesAPI.class,
                new com.alkacode.crates.api.AlkaCratesAPIProvider(keyService, cratesConfig),
                this, org.bukkit.plugin.ServicePriority.Normal);

        getLogger().info("AlkaCrates habilitado com " + cratesConfig.getCrates().size() + " crates.");
    }

    @Override
    protected void onPluginDisable() {
        if (placedCrateManager != null) {
            placedCrateManager.removeAll();
        }
        if (crateService != null) {
            crateService.cancelAllSessions();
        }
    }

    private void registerItemHooks() {
        // AlkaItems (da networking) tem prioridade - eh o item canonico da rede.
        if (getServer().getPluginManager().isPluginEnabled("AlkaItems")) {
            itemHooks.add(new AlkaItemsHook(this));
            getLogger().info("Hook de AlkaItems registrado.");
        }
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            itemHooks.add(new ItemsAdderHook());
            getLogger().info("Hook de ItemsAdder registrado.");
        }
        if (getServer().getPluginManager().isPluginEnabled("Nexo")) {
            itemHooks.add(new NexoHook());
            getLogger().info("Hook de Nexo registrado.");
        }
        if (getServer().getPluginManager().isPluginEnabled("MMOItems")) {
            itemHooks.add(new MMOItemsHook());
            getLogger().info("Hook de MMOItems registrado.");
        }
        if (getServer().getPluginManager().isPluginEnabled("HeadDatabase")) {
            itemHooks.add(new HeadDatabaseHook());
            getLogger().info("Hook de HeadDatabase registrado.");
        }
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            itemHooks.add(new MythicItemsHook());
            getLogger().info("Hook de MythicItems registrado.");
        }
    }

    /**
     * Recarrega config.yml + crates/*.yml e re-coloca todas as crates fisicas. ANTES
     * de re-colocar, remove as instancias atuais (`placedCrateManager.removeAll()`) -
     * sem isso, cada reload duplicava os display entities (ItemDisplay/TextDisplay/
     * Interaction) de toda crate colocada, ja que `loadAll()` sempre spawna instancias
     * novas e nunca limpava as antigas. Usado por `/alkacrates reload` e pelo editor
     * de crates em GUI (toda edicao salva dispara isso pra refletir na hora no mundo).
     */
    public void reloadEverything() {
        reloadConfig();
        cratesConfig.load();
        cratesMessages.reload();
        menuConfig.reload();
        placedCrateManager.removeAll();
        placementService.loadAll();
    }

    public CratesConfig getCratesConfig() { return cratesConfig; }
    public CratesMessages getCratesMessages() { return cratesMessages; }
    public MenuConfig getMenuConfig() { return menuConfig; }
    public GuiLayoutLoader getGuiLayoutLoader() { return guiLayoutLoader; }
    public AnimationEngine getAnimationEngine() { return animationEngine; }
    public PlacedCrateManager getPlacedCrateManager() { return placedCrateManager; }
    public CratePlacementService getPlacementService() { return placementService; }
    public CrateService getCrateService() { return crateService; }
    public KeyService getKeyService() { return keyService; }
    public PityService getPityService() { return pityService; }
    public VirtualKeyRepository getVirtualKeyRepository() { return virtualKeyRepository; }
    public CrateLocationRepository getCrateLocationRepository() { return crateLocationRepository; }
    public PityRepository getPityRepository() { return pityRepository; }
    public CrateLogRepository getCrateLogRepository() { return crateLogRepository; }
    public RewardWinRepository getRewardWinRepository() { return rewardWinRepository; }
    public VirtualKeyManager getVirtualKeyManager() { return virtualKeyManager; }
    public PityManager getPityManager() { return pityManager; }
    public RewardWinManager getRewardWinManager() { return rewardWinManager; }
    public RewardPityManager getRewardPityManager() { return rewardPityManager; }
    public AlkaEconomyHook getEconomyHook() { return economyHook; }
    public CrateFileService getCrateFileService() { return crateFileService; }
    public ChatInputManager getChatInputManager() { return chatInputManager; }
    public List<ItemHook> getItemHooks() { return itemHooks; }
}
