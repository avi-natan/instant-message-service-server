package server.protocol;


/**
 * The Instant Message Service (IMS) Protocol presents 2 static methods,
 * one for converting an array of bytes to an array of strings,
 * and one for converting an array of strings to an array of bytes,
 * both of which represent a message from the IMS client to the server
 * and vice versa.
 * 
 * The protocol makes sure that strings containing a newline or return
 * carriage characters ('\n', '\r') are replaced with a special 
 * "NEWLINEHOLDER" character, in order to prevent a situation where
 * part of the communicated message will not be sent because it
 * contained a new line character - that makes the input stream to stop
 * reading data.
 * 
 * The protocol uses byte -55 (called DELIM) to separate messages and
 * byte -57 (called NEWLINEHOLDER) to indicate newline characters
 * within a message. Every byte array ends with the newline '\n' byte.
 * 
 * For example, a message that represents a correspondence message from
 * user A to user B will be represented as such:
 * 
 * As strings: { "MESSAGE", "A", "Hello,\r\nWorld!" }
 * As byte array:
 * { -55, 77, 69, 83, 83, 65, 71, 69,
 *   -55, 65,
 *   -55, 72, 101, 108, 108, 111, 44, -57, -57, 87, 111, 114, 108, 100, 33, 10 }
 *   
 * 
 * @author Avi
 *
 */
public class IMSProtocol {
	
	private static final byte DELIM = (byte)-55;
	private static final byte NEWLINEHOLDER = (byte)-57;
	
	/**
	 * Takes an array of bytes that represent a message and converts them
	 * to an array of strings, that can be converted back to the same
	 * array of bytes with this protocol.
	 * 
	 * @param bytes - The byte array that represents the message.
	 * @return A string array that represents the message.
	 */
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
	
	/**
	 * Takes an array of strings that represent a message and converts
	 * them to an array of bytes, that can be converted back to the same
	 * array of strings with this protocol.
	 * 
	 * @param message - The string array that represents the message.
	 * @return A byte array that represents the message.
	 */
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