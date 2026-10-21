package com.example.pryanik;
import java.util.Locale;
public enum Rank {
 DEFAULT("§7[Игрок]"), VIP("§a[VIP]"), HELPER("§b[HELPER]"), ADMIN("§c[ADMIN]"), CSRV("§6[CSRV]");
 private final String prefix; Rank(String prefix){this.prefix=prefix;} public String prefix(){return prefix;}
 public static Rank parse(String s){return valueOf(s.toUpperCase(Locale.ROOT));}
 public boolean atLeast(Rank r){return ordinal()>=r.ordinal();}
}
