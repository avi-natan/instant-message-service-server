package server.protocol;

public class IMSProtocol {
	
	private static final byte DELIM = (byte)-55;
	private static final byte NEWLINEHOLDER = (byte)-57;
	
	public static String[] bytesToMessage(byte[] bytes) {
		int stringsCount = 0;
		for(int i = 0; i < bytes.length; i++) {
			if(bytes[i] == DELIM) {
				stringsCount++;
			}
		}
		
		String[] message = new String[stringsCount];
		int stringNum = 0;
		StringBuilder sb = new StringBuilder();
		
		for(int i = 1; i < bytes.length; i++) {
			if(bytes[i] == DELIM) {
				message[stringNum++] = sb.toString();
				sb = new StringBuilder();
			} else {
				if(bytes[i] == NEWLINEHOLDER && bytes[i+1] == NEWLINEHOLDER) {
					sb.append('\r');
					i++;
					sb.append('\n');
				} else if(bytes[i] == NEWLINEHOLDER) {
					sb.append('\n');
				} else {
					sb.append((char) bytes[i]);
				}
			}
		}
		message[stringNum] = sb.toString();
		return message;
	}
	
	public static byte[] messageToBytes(String[] message) {
		int delimCount = message.length;
		int messageLength = 0;
		for(int i = 0; i < message.length; i++) {
			messageLength += message[i].length();
		}
		
		byte[] bytes = new byte[delimCount + messageLength + 1];
		int runner = 0;
		
		for(int i = 0; i < message.length; i++) {
			bytes[runner++] = DELIM;
			byte[] messageIBytes = message[i].getBytes();
			for(int j = 0; j < messageIBytes.length; j++) {
				if(messageIBytes[j] == (byte)10 || messageIBytes[j] == (byte)13) {
					bytes[runner++] = NEWLINEHOLDER;
				} else {
					bytes[runner++] = messageIBytes[j];
				}
			}
		}
		
		bytes[runner] = (byte)10;
		
		return bytes;
	}

}
