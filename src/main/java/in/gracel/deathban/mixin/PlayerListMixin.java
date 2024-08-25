package in.gracel.deathban.mixin;

import com.mojang.authlib.GameProfile;
import in.gracel.deathban.helpers.MessageParser;
import net.minecraft.server.management.BanEntry;
import net.minecraft.server.management.BanList;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.ProfileBanEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Shadow
    public static final File USERBANLIST_FILE = new File("banned-players.json");

    @Shadow
    private BanList bans = new BanList(USERBANLIST_FILE);

    @Inject(at = @At("HEAD"), method = "canPlayerLogin", cancellable = true)
    private void canPlayerLogin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<ITextComponent> callback) {
        if (this.bans.isBanned(profile)) {
            ProfileBanEntry userbanlistentry = this.bans.get(profile);
            if (userbanlistentry.getExpires() != null) {
                LocalDateTime ldtCurr = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault());
                LocalDateTime ldtExpire = LocalDateTime.ofInstant(bans.get(profile).getExpires().toInstant(), ZoneId.systemDefault());
                TranslationTextComponent component = (TranslationTextComponent) MessageParser.banMessage(userbanlistentry.getReason(),
                        MessageParser.getTimeRemaining(ldtCurr, ldtExpire));
                callback.setReturnValue(component);
            }
        }
    }


}
