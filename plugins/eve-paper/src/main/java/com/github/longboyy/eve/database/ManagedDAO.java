package com.github.longboyy.eve.database;

import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.civmodcore.utilities.CivLogger;

public abstract class ManagedDAO {

    protected final ManagedDatasource db;
    protected final CivLogger logger;
    public ManagedDAO(ManagedDatasource db) {
        this.db = db;
        this.logger = CivLogger.getLogger(this.getClass());
    }

    public void updateDatabase(){
        this.registerMigrations();
        this.db.updateDatabase();
    }

    public abstract void registerMigrations();

}
