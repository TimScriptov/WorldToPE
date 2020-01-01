/*
 * Copyright (C) 2019-2020 Тимашков Иван
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.mcal.worldtope;

public class DataLayer {
    public final byte[] data;
    private final int depthBits;
    private final int depthBitsPlusFour;

    public DataLayer(int length, int depthBits) {
        data = new byte[(length >> 1)];
        this.depthBits = depthBits;
        depthBitsPlusFour = depthBits + 4;
    }

    public DataLayer(byte[] data, int depthBits) {
        this.data = data;
        this.depthBits = depthBits;
        depthBitsPlusFour = depthBits + 4;
    }

    public int get(int x, int y, int z) {
        int pos = ((y << depthBitsPlusFour) | (z << this.depthBits)) | x;
        int slot = pos >> 1;
        if ((pos & 1) == 0) {
            return data[slot] & 15;
        }
        return (data[slot] >> 4) & 15;
    }

    public void set(int x, int y, int z, int val) {
        int pos = ((y << depthBitsPlusFour) | (z << depthBits)) | x;
        int slot = pos >> 1;
        if ((pos & 1) == 0) {
            data[slot] = (byte) ((data[slot] & 240) | (val & 15));
        } else {
            data[slot] = (byte) ((data[slot] & 15) | ((val & 15) << 4));
        }
    }

    public boolean isValid() {
        return data != null;
    }

    public void setAll(int br) {
        byte val = (byte) (((br << 4) | br) & 255);
        for (int i = 0; i < data.length; i++) {
            data[i] = val;
        }
    }
}
