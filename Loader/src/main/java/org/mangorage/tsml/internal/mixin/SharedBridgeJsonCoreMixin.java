package org.mangorage.tsml.internal.mixin;

import com.imjustdoom.triviaspire.SharedBridgeJsonCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(SharedBridgeJsonCore.class)
public final class SharedBridgeJsonCoreMixin {
    @Inject(method = "getQuestions", at = @At("RETURN"), cancellable = true)
    private void hookGetQuestions(CallbackInfoReturnable<List<String>> cir) {
        List<String> original = new ArrayList<>(cir.getReturnValue());

        original.add("tsml");

        cir.setReturnValue(original);
    }
}
