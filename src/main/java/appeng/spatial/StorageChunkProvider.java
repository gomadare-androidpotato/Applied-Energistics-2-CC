/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.spatial;


import appeng.api.AEApi;
import appeng.core.AppEng;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkGeneratorOverworld;

import java.util.ArrayList;
import java.util.List;


public class StorageChunkProvider extends ChunkGeneratorOverworld {

    private final World world;

    public StorageChunkProvider(final World world, final long i) {
        super(world, i, false, null);
        this.world = world;
    }

    @Override
    public Chunk generateChunk(final int x, final int z) {
        final Chunk chunk = new Chunk(this.world, x, z);

        final byte[] biomes = chunk.getBiomeArray();
        Biome biome = AppEng.instance().getStorageBiome();
        byte biomeId = (byte) Biome.getIdForBiome(biome);

        for (int k = 0; k < biomes.length; ++k) {
            biomes[k] = biomeId;
        }

        AEApi.instance().definitions().blocks().matrixFrame().maybeBlock().ifPresent(block -> this.fillChunk(chunk, block.getDefaultState()));

        chunk.setModified(false);

        if (!chunk.isTerrainPopulated()) {
            chunk.setTerrainPopulated(true);
            chunk.resetRelightChecks();
        }

        return chunk;
    }

    // 修正版 fillChunk
    private void fillChunk(Chunk chunk, IBlockState defaultState) {
        // 【解説】
        // chunk.setBlockState()を使うと、CubicChunksが破壊した内部クラス(Chunk$1)に触れてクラッシュします。
        // 代わりに、チャンクのデータ保管庫(ExtendedBlockStorage)に直接データを注入します。
        // これにより、照明計算や他MODの干渉をバイパスし、超高速かつ安全に初期化できます。

        net.minecraft.world.chunk.storage.ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();

        // 高さ256ブロック分なので、16個のセクション(16x16x16)を全て埋める
        for (int ySection = 0; ySection < 16; ySection++) {
            // セクションストレージを作成 (ySection << 4 は ySection * 16 の意味)
            net.minecraft.world.chunk.storage.ExtendedBlockStorage storage =
                    new net.minecraft.world.chunk.storage.ExtendedBlockStorage(ySection << 4, true);

            storageArrays[ySection] = storage;

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 16; y++) {
                        // ブロック状態を直接セット (setBlockStateのようなチェック処理が入らないため安全)
                        storage.set(x, y, z, defaultState);
                    }
                }
            }
        }

        // 直接データをいじったので、最後に一度だけ再計算フラグを立てておく（念のため）
        chunk.generateSkylightMap();
        chunk.markDirty();
    }

    @Override
    public void populate(final int par2, final int par3) {

    }

    @Override
    public List getPossibleCreatures(final EnumCreatureType creatureType, final BlockPos pos) {
        return new ArrayList();
    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        return false;
    }

    @Override
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean p_180513_4_) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {

    }
}
