package com.alkacode.crates;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.plugin.AlkaPlugin;
import com.alkacode.crates.config.CratesConfig;
import com.alkacode.crates.animation.AnimationEngine;
import com.alkacode.crates.command.AlkaCratesCommand;
import com.alkacode.crates.command.CrateCommand;
import com.alkacode.crates.crate.placement.PlacedCrateManager;
import com.alkacode.crates.crate.service.CratePlacementService;
import com.alkacode.crates.crate.service.CrateService;
import com.alkacode.crates.crate.service.KeyService;
import com.alkacode.crates.crate.service.PityService;
import com.alkacode.crates.engine.BetterModelSpawner;
import com.alkacode.crates.engine.CraftEngineSpawner;
import com.alkacode.crates.engine.CrateEngineType;
import com.alkacode.crates.engine.ModelEngineSpawner;
import com.alkacode.crates.engine.VanillaEngine;
import com.alkacode.crates.hook.economy.AlkaEconomyHook;
import com.alkacode.crates.hook.item.AlkaItemsHook;
import com.alkacode.crates.hook.item.HeadDatabaseHook;
import com.alkacode.crates.hook.item.ItemHook;
import com.alkacode.crates.hook.item.ItemsAdderHook;
import com.alkacode.crates.hook.item.MMOItemsHook;
import com.alkacode.crates.hook.item.MythicItemsHook;
import com.alkacode.crates.hook.item.NexoHook;
import com.alkacode.crates.listener.CrateInteractionListener;
import com.alkacode.crates.placeholder.CratesExpansion;
import com.alkacode.crates.repository.CrateLocationRepository;
import com.alkacode.crates.repository.CrateLogRepository;
import com.alkacode.crates.repository.PityRepository;
import com.alkacode.crates.repository.VirtualKeyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class AlkaCrates extends AlkaPlugin {

    private CratesConfig cratesConfig;
    private CratesMessages cratesMessages;
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
    private AlkaEconomyHook economyHook;
    private final List<ItemHook> itemHooks = new ArrayList<>();

    @Override
    protected void onPluginEnable() {
        saveDefaultConfig();
        AlkaAPI api = getAlkaAPI();

        this.cratesMessages = new CratesMessages(this);
        this.cratesConfig = new CratesConfig(this);
        cratesConfig.load();

        this.virtualKeyRepository = new VirtualKeyRepository(api.getDatabase());
        this.crateLocationRepository = new CrateLocationRepository(api.getDatabase());
        this.pityRepository = new PityRepository(api.getDatabase());
        this.crateLogRepository = new CrateLogRepository(api.getDatabase());
        try {
            virtualKeyRepository.createTable();
            crateLocationRepository.createTable();
            pityRepository.createTable();
            crateLogRepository.createTable();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Falha ao criar tabelas do AlkaCrates", e);
        }

        this.animationEngine = new AnimationEngine(this);
        this.placedCrateManager = new PlacedCrateManager();
        this.economyHook = new AlkaEconomyHook(getServer());
        registerItemHooks();

        this.placementService = new CratePlacementService(this, new VanillaEngine(this));
        if (getServer().getPluginManager().isPluginEnabled("ModelEngine")) {
            placementService.registerModelSpawner(CrateEngineType.MODELENGINE, new ModelEngineSpawner());
            getLogger().info("Spawner de ModelEngine registrado.");
        }
        if (getServer().getPluginManager().isPluginEnabled("BetterModel")) {
            placementService.registerModelSpawner(CrateEngineType.BETTERMODEL, new BetterModelSpawner());
            getLogger().info("Spawner de BetterModel registrado.");
        }
        if (getServer().getPluginManager().isPluginEnabled("CraftEngine")) {
            placementService.registerModelSpawner(CrateEngineType.CRAFTENGINE, new CraftEngineSpawner());
            getLogger().info("Spawner de CraftEngine registrado.");
        }
        this.crateService = new CrateService(this);
        this.keyService = new KeyService(this, virtualKeyRepository);
        this.pityService = new PityService(this, pityRepository);
        this.crateService.setPityService(pityService);
        this.placementService.loadAll();

        getCommand("crate").setExecutor(new CrateCommand(this));
        getCommand("crate").setTabCompleter(new CrateCommand(this));
        getCommand("alkacrates").setExecutor(new AlkaCratesCommand(this));
        getCommand("alkacrates").setTabCompleter(new AlkaCratesCommand(this));

        getServer().getPluginManager().registerEvents(new CrateInteractionListener(this), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CratesExpansion(this).register();
        }

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

    public CratesConfig getCratesConfig() { return cratesConfig; }
    public CratesMessages getCratesMessages() { return cratesMessages; }
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
    public AlkaEconomyHook getEconomyHook() { return economyHook; }
    public List<ItemHook> getItemHooks() { return itemHooks; }
}
