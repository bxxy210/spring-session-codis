package org.springframework.session.data.codis.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializeUtil {

	private static Logger logger = LoggerFactory.getLogger(SerializeUtil.class);

	public static byte[] serialize(Object object) {
		ObjectOutputStream oos = null;
		ByteArrayOutputStream baos = null;
		try {
			// 序列化
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			byte[] bytes = baos.toByteArray();
			return bytes;
		} catch (Exception e) {
			logger.error("object serialize error!", e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					logger.error("ByteArrayOutputStream close error!", e);
				}
			}
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					logger.error("ObjectOutputStream close error!", e);
				}
			}
		}
		return null;
	}

	public static Object deserialize(byte[] bytes) {
		ByteArrayInputStream bais = null;
		ObjectInputStream ois = null;
		try {
			// 反序列化
			bais = new ByteArrayInputStream(bytes);
			ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (Exception e) {
			logger.error("object unSerialize error!", e);
		} finally {
			if (bais != null) {
				try {
					bais.close();
				} catch (IOException e) {
					logger.error("ByteArrayInputStream close error!", e);
				}
			}
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					logger.error("ObjectInputStream close error!", e);
				}
			}
		}
		return null;
	}
}
