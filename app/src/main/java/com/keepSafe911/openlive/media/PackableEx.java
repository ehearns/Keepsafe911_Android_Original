package com.keepSafe911.openlive.media;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
