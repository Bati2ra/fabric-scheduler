package com.bati.fabricscheduler.mixin;

import com.bati.fabricscheduler.FabricScheduler;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftDedicatedServer.class)
public class MixinDedicatedServer {
    /**
     * <a href="https://images-wixmp-ed30a86b8c4ca887773594c2.wixmp.com/f/cc10d57c-1722-4a2b-a40f-9ce65103dda9/dfalmc6-446e727b-11e6-46dc-85ec-92f7a998dda9.png/v1/fill/w_720,h_715/mr_incredible_becoming_uncanny__phase_3__by_wksnfbbrndkdofcoclcl_dfalmc6-fullview.png?token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1cm46YXBwOjdlMGQxODg5ODIyNjQzNzNhNWYwZDQxNWVhMGQyNmUwIiwiaXNzIjoidXJuOmFwcDo3ZTBkMTg4OTgyMjY0MzczYTVmMGQ0MTVlYTBkMjZlMCIsIm9iaiI6W1t7ImhlaWdodCI6Ijw9NzE1IiwicGF0aCI6IlwvZlwvY2MxMGQ1N2MtMTcyMi00YTJiLWE0MGYtOWNlNjUxMDNkZGE5XC9kZmFsbWM2LTQ0NmU3MjdiLTExZTYtNDZkYy04NWVjLTkyZjdhOTk4ZGRhOS5wbmciLCJ3aWR0aCI6Ijw9NzIwIn1dXSwiYXVkIjpbInVybjpzZXJ2aWNlOmltYWdlLm9wZXJhdGlvbnMiXX0.mTvlRNMvAm9hcTZA1kwcXHZb_wIxcajt2sQRLoCIXZw">Me right now</a>
     * I don't know where to start.<br>
     * Issue: Threads of asynchronous tasks locks sometimes.<br>
     * In short: Shutdown process stops the Scheduler tasks successfully and, in theory, also cancels the running Threads after a set time,
     * but the console freezes when using the 'stop' command. Only happens with the batch console.
     * And it seems to happen only with some tasks.
     * If the server got to this point, it means that it already saved everything successfully and ran the shutdown processes,
     * so killing the process this way should not cause any errors, and if it does, it would only be with blocked Threads (tasks).

     */
    @Inject(at = @At("TAIL"), method = "exit")
    public void killProcess(CallbackInfo ci) {
        FabricScheduler.LOGGER.info("Goodbye!");
        Runtime.getRuntime().halt(0);
    }
}
