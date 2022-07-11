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

import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LT_Name
 */
public class ChuckMoveUpHelper extends PluginBase {

    private final Set<Vector2> modifyChuck = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

        boolean isUp = true;
        int movedY = 0;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("up")) {
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

        if (movedY >= 0) {
            isUp = true;
        }else {
            isUp = false;
        }

        Player player = (Player) sender;
        Level level = player.getLevel();
        Vector2 vector2 = new Vector2(player.getChunkX()<<4, player.getChunkZ()<<4);
        if (this.modifyChuck.contains(vector2)) {
            sender.sendMessage("此区块正在修改中，请等待修改完成！");
            return true;
        }
        this.modifyChuck.add(vector2);
        sender.sendMessage("正在尝试修改区块(所有方块y移动"+ movedY + ")，请稍后...");

        final boolean finalIsUp = isUp;
        final int finalMovedY = movedY;
        this.getServer().getScheduler().scheduleAsyncTask(this, new AsyncTask() {
            @Override
            public void onRun() {
                if (finalIsUp) {
                    for (int y = 255; y >= -64; y--) {
                        moveLayerBlock(level, vector2, y, finalMovedY);
                    }
                }else {
                    for (int y = 0; y <= 384; y++) {
                        moveLayerBlock(level, vector2, y, finalMovedY);
                    }
                }
                modifyChuck.remove(vector2);
                sender.sendMessage("区块修改完成！");
            }
        });
        return true;
    }

    private void moveLayerBlock(Level level, Vector2 vector2, int y, int movedY) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Vector3 vector3 = new Vector3(vector2.x + x, y, vector2.y + z);
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
    @AllArgsConstructor
    public static class BlockData {

        private Vector3 vector3;
        private Block block;

    }

}
