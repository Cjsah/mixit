package net.cjsah.mod.carpet.script.api;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.cjsah.mod.carpet.script.CarpetContext;
import net.cjsah.mod.carpet.script.Expression;
import net.cjsah.mod.carpet.script.exception.InternalExpressionException;
import net.cjsah.mod.carpet.script.utils.SystemInfo;
import net.cjsah.mod.carpet.script.value.ListValue;
import net.cjsah.mod.carpet.script.value.MapValue;
import net.cjsah.mod.carpet.script.value.NumericValue;
import net.cjsah.mod.carpet.script.value.StringValue;
import net.cjsah.mod.carpet.script.value.Value;
import net.cjsah.mod.carpet.utils.SpawnReporter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Monitoring {

    public static void apply(Expression expression) {
        expression.addContextFunction("system_info", -1, (c, t, lv) -> {
            if (lv.size() == 0) {
                return SystemInfo.getAll();
            }
            if (lv.size() == 1) {
                String what = lv.get(0).getString();
                Value res = SystemInfo.get(what, (CarpetContext) c);
                if (res == null) throw new InternalExpressionException("Unknown option for 'system_info': " + what);
                return res;
            }
            throw new InternalExpressionException("'system_info' requires one or no parameters");
        });
        // game processed snooper functions
        expression.addContextFunction("get_mob_counts", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            ServerLevel world = cc.s.getLevel();
            NaturalSpawner.SpawnState info = world.getChunkSource().getLastSpawnState();
            if (info == null) return Value.NULL;
            Object2IntMap<MobCategory> mobcounts = info.getMobCategoryCounts();
            int chunks = info.getSpawnableChunkCount();
            if (lv.size() == 0) {
                Map<Value, Value> retDict = new HashMap<>();
                for (MobCategory category: mobcounts.keySet()) {
                    int currentCap = (int)(category.getMaxInstancesPerChunk() * chunks / SpawnReporter.MAGIC_NUMBER);
                    retDict.put(
                            new StringValue(category.getSerializedName().toLowerCase(Locale.ROOT)),
                            ListValue.of(
                                    new NumericValue(mobcounts.getInt(category)),
                                    new NumericValue(currentCap))
                    );
                }
                return MapValue.wrap(retDict);
            }
            String catString = lv.get(0).getString();
            MobCategory cat = MobCategory.byName(catString.toLowerCase(Locale.ROOT));
            if (cat == null) throw new InternalExpressionException("Unreconized mob category: "+catString);
            return ListValue.of(
                    new NumericValue(mobcounts.getInt(cat)),
                    new NumericValue((int)(cat.getMaxInstancesPerChunk() * chunks / SpawnReporter.MAGIC_NUMBER))
            );
        });
    }
}
