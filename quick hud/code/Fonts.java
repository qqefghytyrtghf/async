package tech.renatclient.util.render.msdf;

import com.google.common.base.Suppliers;

import java.util.function.Supplier;

public class Fonts {
    public static final Supplier<MsdfFont> SFBOLD = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
    public static final Supplier<MsdfFont> SFMEDIUM = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    public static final Supplier<MsdfFont> ICONS_HUD = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons_nurik").data("icons_nurik").build());
    public static final Supplier<MsdfFont> QUICK = Suppliers.memoize(() -> MsdfFont.builder().atlas("quick").data("quick").build());
}
