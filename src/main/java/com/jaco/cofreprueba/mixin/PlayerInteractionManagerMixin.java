package com.jaco.cofreprueba.mixin;

import com.jaco.cofreprueba.CofrePruebaMod;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.server.network.ServerPlayerInteractionManager.class)
public class PlayerInteractionManagerMixin {

    @Inject(method = "interactBlock", at = @At("HEAD"))
    public void onBlockPlace(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (stack.getItem() == Items.CHEST) {
            BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
            CofrePruebaMod.onChestPlaced(player, world, pos);
        }
    }
}
