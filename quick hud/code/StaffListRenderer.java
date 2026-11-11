package tech.renatclient.module.list.render.hud.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import tech.renatclient.module.list.render.hud.Interface;
import tech.renatclient.util.render.math.Animation;
import tech.renatclient.util.render.math.Easing;
import tech.renatclient.util.render.providers.ColorProvider;
import tech.renatclient.util.replace.ReplaceUtil;
import tech.renatclient.util.staff.StaffManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class StaffListRenderer {
    private final Interface owner;
    private final List<Staff> staffPlayers = new ArrayList<>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern prefixMatches = Pattern.compile(".*(ꔷ|ꔳ|ꔩ|ꔥ|ꔡ|ꔗ|ꔓ|\\bmod\\b|\\badm\\b|\\bhelp\\b|\\bwne\\b|модер|хелп|помощ|админ|владел|отриц|\\btaf\\b|\\bcurat\\b|куратор|\\bdev\\b|разраб|\\bsupp\\b|саппорт|\\byt\\b|\\[yt\\]|ютуб|стажер|сотрудник).*");
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 200);
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);

    public StaffListRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;
        renderQuick(context);
    }

    private void renderQuick(DrawContext context) {
        final boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        List<QuickStyle.Row> rows = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Staff staff : staffPlayers) {
            float anim = staff.animation.run(staff.isOnServer ? 1f : 0f);
            if (anim <= 0.01f) continue;

            PlayerListEntry entry = owner.mc.getNetworkHandler() != null ? owner.mc.getNetworkHandler().getPlayerListEntry(staff.name) : null;
            Identifier skin = entry != null ? entry.getSkinTextures().texture() : DefaultSkinHelper.getTexture();
            String time = formatQuickDuration(now - staff.firstSeen);
            rows.add(staff.status == Status.VANISHED
                    ? QuickStyle.Row.player(staff.name, "Vanish", QuickStyle.LEVEL_COLOR, time, anim, skin)
                    : QuickStyle.Row.player(staff.name, time, 0, null, anim, skin));
        }

        int a = QuickStyle.toAlpha(alpha.run(rows.isEmpty() && !chatOpen ? 0f : 1f));
        if (a <= 4) return;
        QuickStyle.drawList(context, owner.getStaffListDrag(), widthAnim, QuickStyle.ICON_STAFF, "StaffList", rows, a);
    }

    private static String formatQuickDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        if (seconds < 60L) return seconds + "s";
        if (seconds < 3600L) return (seconds / 60L) + "m";
        return (seconds / 3600L) + "h";
    }

    public void update() {
        for (Staff staff : staffPlayers) {
            staff.isOnServer = false;
        }

        for (PlayerListEntry playerListEntry : owner.mc.getNetworkHandler().getPlayerList()) {
            String name = playerListEntry.getProfile().getName().replaceAll("[\\[\\]]", "");
            PlayerListEntry info = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(name);
            boolean vanish = info == null;
            boolean isGM3 = info != null && info.getGameMode() == net.minecraft.world.GameMode.SPECTATOR;

            boolean matchesPrefix = prefixMatches.matcher(playerListEntry.getDisplayName() != null ? playerListEntry.getDisplayName().getString().toLowerCase(Locale.ROOT) : "").matches();
            boolean isValidName = namePattern.matcher(name).matches();
            boolean notSelf = !name.equals(MinecraftClient.getInstance().player.getName().getString());

            if ((isValidName && notSelf && matchesPrefix) || (isValidName && notSelf && vanish) || StaffManager.isStaff(name)) {
                if (StaffManager.isStaff(name)) {
                    String[] names = new String[]{"auction", "exp_smith", "shop_balls", "shop_grief", "free", "shop_kits", "siege", "rwplus", "bossfight", "guide", "shop_smith", "shop_spawners", "colliseum", "battlepass", "buyer", "huckster", "buff_brewer", "killer", "shop_mage"};
                    boolean contains = false;
                    if (MinecraftClient.getInstance().getCurrentServerEntry() != null && MinecraftClient.getInstance().getCurrentServerEntry().address != null
                            && (MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.rwdonat.pw")
                            || MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.cakeworld.pw"))) {
                        for (String n : names) {
                            if (name.contains(n)) {
                                contains = true;
                                break;
                            }
                        }
                    }
                    if (contains) continue;
                }

                Optional<Staff> existingStaff = staffPlayers.stream().filter(s -> s.name.equals(name)).findFirst();
                Status status = vanish ? Status.VANISHED : (isGM3 ? Status.VANISHED : Status.NONE);

                if (existingStaff.isPresent()) {
                    Staff s = existingStaff.get();
                    s.isOnServer = true;
                    s.status = status;
                } else {
                    String[] names = new String[]{"auction", "exp_smith", "shop_balls", "shop_grief", "free", "shop_kits", "siege", "rwplus", "bossfight", "guide", "shop_smith", "shop_spawners", "colliseum", "battlepass", "buyer", "huckster", "buff_brewer", "killer", "shop_mage"};
                    boolean contains = false;
                    if (MinecraftClient.getInstance().getCurrentServerEntry() != null && MinecraftClient.getInstance().getCurrentServerEntry().address != null
                            && (MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.rwdonat.pw")
                            || MinecraftClient.getInstance().getCurrentServerEntry().address.contains("mc.cakeworld.pw"))) {
                        for (String n : names) {
                            if (name.contains(n)) {
                                contains = true;
                            }
                        }
                    }
                    if (!contains) {
                        net.minecraft.text.Text originalPrefix = playerListEntry.getDisplayName();
                        net.minecraft.text.Text prefix = originalPrefix;
                        if (prefix != null) {
                            prefix = ReplaceUtil.replaceSymbols(prefix);
                            String fullString = prefix.getString();
                            int nickIndex = fullString.indexOf(name);
                            if (nickIndex != -1) {
                                int endIndex = nickIndex + name.length();
                                if (endIndex < fullString.length()) {
                                    net.minecraft.text.MutableText newText = net.minecraft.text.Text.empty();
                                    int currentLength = 0;
                                    net.minecraft.text.MutableText baseCopy = prefix.copy();
                                    baseCopy.getSiblings().clear();
                                    String mainContent = baseCopy.getString();
                                    if (!mainContent.isEmpty() && currentLength < endIndex) {
                                        int takeLength = Math.min(mainContent.length(), endIndex - currentLength);
                                        newText.append(net.minecraft.text.Text.literal(mainContent.substring(0, takeLength)).setStyle(prefix.getStyle()));
                                        currentLength += takeLength;
                                    }
                                    for (net.minecraft.text.Text sibling : prefix.getSiblings()) {
                                        if (currentLength >= endIndex) break;
                                        net.minecraft.text.MutableText siblingCopy = sibling.copy();
                                        siblingCopy.getSiblings().clear();
                                        String siblingContent = siblingCopy.getString();
                                        int takeLength = Math.min(siblingContent.length(), endIndex - currentLength);
                                        if (takeLength > 0) {
                                            newText.append(net.minecraft.text.Text.literal(siblingContent.substring(0, takeLength)).setStyle(sibling.getStyle()));
                                            currentLength += takeLength;
                                        }
                                    }
                                    prefix = newText;
                                }
                            }
                        }
                        Staff staff = new Staff(prefix == null ? net.minecraft.text.Text.of(playerListEntry.getProfile().getName()) : prefix, name, vanish || isGM3, status);
                        staff.isOnServer = true;
                        staffPlayers.add(staff);
                    }
                }
            }
        }
        staffPlayers.removeIf(staff -> !staff.isOnServer && staff.animation.getValue() == 0);
    }

    private enum Status {
        NONE("", -1),
        VANISHED("SPEC", ColorProvider.rgba(229, 0, 63, 255));

        private final String string;
        private final int color;

        Status(String string, int color) {
            this.string = string;
            this.color = color;
        }
    }

    private static class Staff {
        net.minecraft.text.Text prefix;
        String name;
        boolean isSpec;
        Status status;
        boolean isOnServer;
        final long firstSeen = System.currentTimeMillis();
        Animation animation = new Animation(Easing.EXPO_OUT, 233);

        Staff(net.minecraft.text.Text prefix, String name, boolean isSpec, Status status) {
            this.prefix = prefix;
            this.name = name;
            this.isSpec = isSpec;
            this.status = status;
        }
    }
}
