package cn.lanink.chuckmoveuphelper;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.AsyncTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LT_Name
 */
public class ChuckMoveUpHelper extends PluginBase {

    private final Set<Vector2> modifyChuck = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final HashMap<Player, PlayerCloseRegion> playerChoosePos = new HashMap<>();

    @Override
    public void onEnable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("请在游戏内使用此命令！");
            return true;
        }
        if (!sender.isOp()) {
            sender.sendMessage("你没有权限使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        Level level = player.getLevel();

        boolean isUp = true;
        int movedY = 0;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("pos1")) {
                PlayerCloseRegion region = this.playerChoosePos.getOrDefault(player, new PlayerCloseRegion());
                region.setMinX(player.getFloorX()).setMinZ(player.getFloorZ());
                this.playerChoosePos.put(player, region);
                sender.sendMessage("请使用 /ChuckMoveUp pos2 设置第二个坐标");
                return true;
            }else if (args[0].equalsIgnoreCase("pos2")) {
                PlayerCloseRegion region = this.playerChoosePos.getOrDefault(player, new PlayerCloseRegion());
                region.setMaxX(player.getFloorX()).setMaxZ(player.getFloorZ());
                this.playerChoosePos.put(player, region);
                sender.sendMessage("请使用 /ChuckMoveUp [moveY] 调整区块");
                return true;
            }else if (args[0].equalsIgnoreCase("up")) {
                isUp = true;
            }else if (args[0].equalsIgnoreCase("down")) {
                isUp = false;
            }else {
                try {
                    movedY = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("请输入正确的数字！");
                    return false;
                }
            }
        }

        if (movedY == 0) {
            movedY = (isUp ? 64 : -64);
        }

        HashSet<ChuckXZ> chuckXZList = new HashSet<>();
        PlayerCloseRegion region = this.playerChoosePos.get(player);
        if (region != null) {
            region.check();
            for (int x = region.getMinX(); x <= region.getMaxX(); x++) {
                for (int z = region.getMinZ(); z <= region.getMaxZ(); z++) {
                    chuckXZList.add(new ChuckXZ(x>>4, z>>4));
                }
            }
            this.playerChoosePos.remove(player);
        }else {
            //未选择默认玩家脚下区块
            chuckXZList.add(new ChuckXZ(player.getChunkX(), player.getChunkZ()));
        }

        sender.sendMessage(chuckXZList.toString());

        sender.sendMessage("正在尝试修改区块(所有方块y移动" + movedY + ")，请稍后...");
        for (ChuckXZ chuckXZ : chuckXZList) {
            Vector2 startVector2 = new Vector2(chuckXZ.getX() << 4, chuckXZ.getZ() << 4);
            if (this.modifyChuck.contains(startVector2)) {
                return true;
            }
            this.modifyChuck.add(startVector2);

            this.moveChuck(player, level, startVector2, movedY);
        }
        return true;
    }

    private void moveChuck(Player player, Level level, Vector2 startVector2, int movedY) {
        final boolean finalIsUp;
        if (movedY >= 0) {
            finalIsUp = true;
        }else {
            finalIsUp = false;
        }
        final int finalMovedY = movedY;
        this.getServer().getScheduler().scheduleAsyncTask(this, new AsyncTask() {
            @Override
            public void onRun() {
                if (finalIsUp) {
                    for (int y = 255; y >= -64; y--) {
                        moveLayerBlock(level, startVector2, y, finalMovedY);
                    }
                }else {
                    for (int y = 0; y <= 384; y++) {
                        moveLayerBlock(level, startVector2, y, finalMovedY);
                    }
                }
                modifyChuck.remove(startVector2);
                player.sendMessage("区块[" + (startVector2.getFloorX()>>4)  + ":" + (startVector2.getFloorY()>>4) + "]修改完成！");
            }
        });
    }

    private void moveLayerBlock(Level level, Vector2 startVector2, int y, int movedY) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Vector3 vector3 = new Vector3(startVector2.x + x, y, startVector2.y + z);
                moveBlock(level, vector3, movedY);
            }
        }
    }

    private void moveBlock(Level level, Vector3 pos, int movedY) {
        level.setBlockStateAt(
                pos.getFloorX(),
                pos.getFloorY() + movedY,
                pos.getFloorZ(),
                0,
                level.getBlockStateAt(
                        pos.getFloorX(),
                        pos.getFloorY(),
                        pos.getFloorZ(),
                        0
                )
        );
        level.setBlockStateAt(
                pos.getFloorX(),
                pos.getFloorY() + movedY,
                pos.getFloorZ(),
                1,
                level.getBlockStateAt(
                        pos.getFloorX(),
                        pos.getFloorY(),
                        pos.getFloorZ(),
                        1
                )
        );
    }

    @Data
    public static class PlayerCloseRegion {
        private Integer minX;
        private Integer maxX;
        private Integer minZ;
        private Integer maxZ;

        public PlayerCloseRegion setMinX(Integer minX) {
            this.minX = minX;
            return this;
        }

        public PlayerCloseRegion setMaxX(Integer maxX) {
            this.maxX = maxX;
            return this;
        }

        public PlayerCloseRegion setMinZ(Integer minZ) {
            this.minZ = minZ;
            return this;
        }

        public PlayerCloseRegion setMaxZ(Integer maxZ) {
            this.maxZ = maxZ;
            return this;
        }

        public void check() {
            Integer minXCache = this.minX;
            Integer maxXCache = this.maxX;
            Integer minZCache = this.minZ;
            Integer maxZCache = this.maxZ;

            this.minX = Math.min(minXCache, maxXCache);
            this.maxX = Math.max(minXCache, maxXCache);
            this.minZ = Math.min(minZCache, maxZCache);
            this.maxZ = Math.max(minZCache, maxZCache);
        }
    }

    @Data
    @EqualsAndHashCode
    public static class ChuckXZ {
        public int x;
        public int z;

        public ChuckXZ(int x, int z) {
            this.x = x;
            this.z = z;
        }

    }

    @Data
    @AllArgsConstructor
    public static class BlockData {

        private Vector3 vector3;
        private Block block;

    }

}
