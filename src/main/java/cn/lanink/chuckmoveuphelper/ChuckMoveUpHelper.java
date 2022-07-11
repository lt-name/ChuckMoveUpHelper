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

    private Set<Vector2> modifyChuck = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

        boolean up = true;

        Player player = (Player) sender;
        Level level = player.getLevel();
        Vector2 vector2 = new Vector2(player.getChunkX()<<4, player.getChunkZ()<<4);
        if (this.modifyChuck.contains(vector2)) {
            sender.sendMessage("此区块正在修改中，请等待修改完成！");
            return true;
        }
        this.modifyChuck.add(vector2);
        sender.sendMessage("正在尝试修改区块，请稍后...");
        this.getServer().getScheduler().scheduleAsyncTask(this, new AsyncTask() {
            @Override
            public void onRun() {
                LinkedList<BlockData> list = new LinkedList<>();
                for (int y = 255; y > -64; y--) {
                    for (int x = 0; x < 16; x++) {
                        final int finalX = x;
                        final int finalY = y;
                        for (int z = 0; z < 16; z++) {
                            Vector3 vector3 = new Vector3(vector2.x + finalX, finalY, vector2.y + z);
                            Block block = level.getBlock(vector3);
                            list.add(new BlockData(vector3, block));
                            level.setBlock(vector3.add(0, up ? 64 : -64, 0), block);
                        }
                    }
                }
                while (!list.isEmpty()) {
                    BlockData blockData = list.pollLast();
                    level.setBlock(blockData.vector3.add(0, up ? 64 : -64, 0), blockData.block);
                }
                modifyChuck.remove(vector2);
                sender.sendMessage("区块修改完成！");
            }
        });
        return true;
    }

    @Data
    @AllArgsConstructor
    public static class BlockData {

        private Vector3 vector3;
        private Block block;

    }

}
