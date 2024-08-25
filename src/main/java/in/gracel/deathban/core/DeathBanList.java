package in.gracel.deathban.core;

import com.mojang.authlib.GameProfile;
import in.gracel.deathban.DeathBan;
import in.gracel.deathban.helpers.MessageParser;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.BanList;
import net.minecraft.server.management.ProfileBanEntry;
import net.minecraft.util.text.TranslationTextComponent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DeathBanList {
    private BanList userBanList;

    public DeathBanList(BanList banList) {
        this.userBanList = banList;
    }

    public void addToBanList(MinecraftServer server, GameProfile deadPlayer, Date expire, String reason) {
        if (!userBanList.isBanned(deadPlayer)) {
            ServerPlayerEntity serverplayer = server.getPlayerList().getPlayer(deadPlayer.getId());
            ProfileBanEntry entry = new ProfileBanEntry(deadPlayer, new Date(), DeathBan.MOD_NAME, expire, reason);
            userBanList.add(entry);
            LocalDateTime ldtCurr = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault());
            LocalDateTime ldtExpire = LocalDateTime.ofInstant(expire.toInstant(), ZoneId.systemDefault());

            TranslationTextComponent component = (TranslationTextComponent) MessageParser.banMessage(reason,
                    MessageParser.getTimeRemaining(ldtCurr, ldtExpire));
            assert serverplayer != null;
            serverplayer.connection.disconnect(component);
        }
    }

    public void removeBanIfTimeExpire() {
        Date currDate = new Date();
        userBanList.getEntries().removeIf(entry -> currDate.after((entry.getExpires())));
    }

    public void removeAll() {
        userBanList.getEntries().clear();
    }
}
