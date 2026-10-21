package com.example.pryanik;
import java.util.Locale;
import java.util.Arrays;
public enum Rank {
 DEFAULT("§7[Игрок]"), VIP("§a[VIP]"), ADMIN("§c[ADMIN]"), HELPER("§b[HELPER]"), CSRV("§6[CSRV]");
 private final String prefix; Rank(String prefix){this.prefix=prefix;} public String prefix(){return prefix;}
 public static Rank parse(String s){return valueOf(s.toUpperCase(Locale.ROOT));}
 public boolean atLeast(Rank r){return ordinal()>=r.ordinal();} public boolean canUseWorld(){return this==CSRV;} public boolean canSetRank(){return this==CSRV;} public boolean canUseVanish(){return this==CSRV||this==HELPER;} public static Rank[] assignableBy(Rank setter){return Arrays.stream(values()).filter(r->r.ordinal()<setter.ordinal()).toArray(Rank[]::new);}
}
