package dev.foxikle.foxrankvelocity;

import java.util.UUID;

public class FoxRankAPI {
    private final FoxRankVelocity plugin = FoxRankVelocity.INSTANCE;

    public static Rank getPlayerRank(UUID uuid) {
        return FoxRankVelocity.INSTANCE.dataManager.getStoredRank(uuid);
    }
}
