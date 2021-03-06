package net.zhuoweizhang.pocketinveditor.io.nbt.tileentity;

import java.util.ArrayList;
import java.util.List;
import net.zhuoweizhang.pocketinveditor.tileentity.TileEntity;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;
import org.spout.nbt.Tag;


public class TileEntityStore<T extends TileEntity> {

	public void load(T entity, CompoundTag tag) {
		List<Tag> tags = tag.getValue();
		for (Tag t: tags) {
			loadTag(entity, t);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadTag(T entity, Tag tag) {
		if (tag.getName().equals("x")) {
			entity.setX(((IntTag) tag).getValue());
		} else if (tag.getName().equals("y")) {
			entity.setY(((IntTag) tag).getValue());
		} else if (tag.getName().equals("z")) {
			entity.setZ(((IntTag) tag).getValue());
		} else if (tag.getName().equals("id")) {
			entity.setId(((StringTag) tag).getValue());
		} else {
			System.out.println("Unhandled tag " + tag.getName() + ":" + tag.toString() + " for tile entity " + this);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Tag> save(T entity) {
		List<Tag> tags = new ArrayList<Tag>();

		tags.add(new StringTag("id", entity.getId()));
		tags.add(new IntTag("x", entity.getX()));
		tags.add(new IntTag("y", entity.getY()));
		tags.add(new IntTag("z", entity.getZ()));

		return tags;
	}
}

