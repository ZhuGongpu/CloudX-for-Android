package common.message;

import com.google.protobuf.ByteString;
import cloudx.utils.ByteStringUtils;
import cloudx.utils.TimeUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by zhugongpu on 14/11/13.
 */
public class ProtoBufHelper {

    /**
     * 将data packet写入到流中(delimited)
     *
     * @param dataPacket
     * @param stream
     * @throws IOException
     */
    private static void sendDataPacket(Data.DataPacket.Builder dataPacket, OutputStream stream) throws IOException {
        sendDataPacket(dataPacket.build(), stream);
    }

    /**
     * 将data packet写入到流中(delimited)
     *
     * @param dataPacket
     * @param stream
     * @throws IOException
     */
    private static void sendDataPacket(Data.DataPacket dataPacket, OutputStream stream) throws IOException {
        dataPacket.writeDelimitedTo(stream);
    }

    /**
     * 将command写入流中
     *
     * @param command
     * @param stream
     */
    public static void sendCommand(Data.Command.Builder command, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Command).setCommand(command), stream);
    }

    /**
     * 将video写入流中
     *
     * @param video
     * @param stream
     * @throws IOException
     */
    public static void sendVideo(Data.Video.Builder video, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Video).setVideo(video), stream);
    }

    /**
     * 将audio写入流中
     *
     * @param audio
     * @param stream
     * @throws IOException
     */
    public static void sendAudio(Data.Audio.Builder audio, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Audio).setAudio(audio), stream);
    }

    /**
     * 将device info写入流中
     *
     * @param deviceInfo
     * @param stream
     * @throws IOException
     */
    public static void sendDeviceInfo(Data.DeviceInfo.Builder deviceInfo, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.DeviceInfo).setDeviceInfo(deviceInfo), stream);
    }

    /**
     * 将file request写入流中
     *
     * @param request
     * @param stream
     * @throws IOException
     */
    public static void sendFileRequest(Data.FileRequest.Builder request, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.FileRequest).setFileRequest(request), stream);
    }

    /**
     * 将file info写入流中
     *
     * @param fileInfo
     * @param stream
     * @throws IOException
     */
    public static void sendFileInfo(Data.FileInfo.Builder fileInfo, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.FileInfo).setFileInfo(fileInfo), stream);
    }

    /**
     * 将file block写入流中
     *
     * @param fileBlock
     * @param stream
     * @throws IOException
     */
    public static void sendFileBlock(Data.FileBlock.Builder fileBlock, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.FileBlock), stream);
    }

    /**
     * 将token request写入流中
     *
     * @param request
     * @param stream
     * @throws IOException
     */
    public static void sendCloudStorageTokenRequest(Data.CloudStorageTokenRequest.Builder request, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.CloudStorageTokenRequest).setCloudStorageTokenRequest(request), stream);
    }

    /**
     * 将token写入流中
     *
     * @param token
     * @param stream
     * @throws IOException
     */
    public static void sendCloudStorageToken(Data.CloudStorageToken.Builder token, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.CloudStorageToken).setCloudStorageToken(token), stream);
    }

    /**
     * 将message写入流中
     *
     * @param message
     * @param stream
     * @throws IOException
     */
    public static void sendSharedMessage(Data.SharedMessage.Builder message, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.SharedMessage).setSharedMessage(message), stream);
    }

    /**
     * 将keyboard event写入流中
     *
     * @param keyboardEvent
     * @param stream
     * @throws IOException
     */
    public static void sendKeyboardEvent(Data.KeyboardEvent.Builder keyboardEvent, OutputStream stream) throws IOException {
        sendDataPacket(genNewDataPacketBuilder().setDataPacketType(Data.DataPacket.DataPacketType.KeyboardEvent).setKeyboardEvent(keyboardEvent), stream);
    }

    /**
     * 生成带有时间戳的builder
     *
     * @return
     */
    private static Data.DataPacket.Builder genNewDataPacketBuilder() {
        return Data.DataPacket.newBuilder().setUnixTimeStamp(TimeUtils.getCurrentTimeStamp());
    }


    /**
     * 生成只带type信息的command
     *
     * @param type
     * @return
     */
    public static Data.Command.Builder genCommandBuilder(Data.Command.CommandType type) {
        return Data.Command.newBuilder().setCommandType(type);
    }

    /**
     * 生成带有type、x信息的command
     *
     * @param type
     * @param x
     * @return
     */
    public static Data.Command.Builder genCommandBuilder(Data.Command.CommandType type, float x) {
        return genCommandBuilder(type).setX(x);
    }

    /**
     * 生成完整的command
     *
     * @param type
     * @param x
     * @param y
     * @return
     */
    public static Data.Command.Builder genCommand(Data.Command.CommandType type, float x, float y) {
        return genCommandBuilder(type, x);
    }


    /**
     * 生成不带image的rect
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
    public static Data.Video.Rectangle.Builder genRectangleBuilder(int x, int y, int width, int height) {
        return Data.Video.Rectangle.newBuilder().setHeight(height).setWidth(width).setX(x).setY(y);
    }

    /**
     * 生成带有image的rect
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * @param image
     * @return
     */
    public static Data.Video.Rectangle.Builder genRectangle(int x, int y, int width, int height, ByteString image) {
        return genRectangleBuilder(x, y, width, height).setImage(image);
    }

    /**
     * 生成point
     *
     * @param x
     * @param y
     * @return
     */
    private static Data.Video.Point.Builder genPoint(int x, int y) {
        return Data.Video.Point.newBuilder().setY(y).setX(x);
    }

    /**
     * 生成move rect
     *
     * @param destRect 不带有image的rect
     * @param srcPoint
     * @return
     */
    public static Data.Video.MoveRectangle.Builder genMoveRetangle(Data.Video.Rectangle destRect, Data.Video.Point srcPoint) {
        return Data.Video.MoveRectangle.newBuilder().setDestinationRectangle(destRect).setSourcePoint(srcPoint);
    }

    /**
     * @return
     */
    private static Data.Video.Builder genNewVideoBuilder() {
        return Data.Video.newBuilder();
    }

    /**
     * 生成只带有dirty rect、move rect的video
     *
     * @param dirtyRects 可为null
     * @param moveRects  可为null
     * @return
     */
    public static Data.Video.Builder genVideo(List<Data.Video.Rectangle> dirtyRects, List<Data.Video.MoveRectangle> moveRects) {
        Data.Video.Builder builder = genNewVideoBuilder();
        if (dirtyRects != null && dirtyRects.size() != 0)
            builder.addAllDirtyRects(dirtyRects);
        if (moveRects != null && moveRects.size() != 0)
            builder.addAllMoveRects(moveRects);
        return builder;
    }

    /**
     * 生成只带有frame的video
     *
     * @param frame
     * @return
     */
    public static Data.Video.Builder genVideo(ByteString frame) {
        return genNewVideoBuilder().setFrame(frame);
    }


    /**
     * 生成audio builder
     *
     * @param sound
     * @return
     */
    public static Data.Audio.Builder genAudio(ByteString sound) {
        return Data.Audio.newBuilder().setSound(sound);
    }


    /**
     * 生成resolution builder
     *
     * @param width
     * @param height
     * @return
     */
    public static Data.Resolution.Builder genResolution(int width, int height) {
        return Data.Resolution.newBuilder().setWidth(width).setHeight(height);
    }

    /**
     * 生成完整的deviceInfo builder
     *
     * @param deviceName
     * @param portAvailable
     * @param resolution
     * @return
     */
    public static Data.DeviceInfo.Builder genDeviceInfo(ByteString deviceName, int portAvailable, Data.Resolution.Builder resolution) {
        return Data.DeviceInfo.newBuilder().setDeviceName(deviceName).setPortAvailable(portAvailable).setResolution(resolution);
    }

    /**
     * 生成完整的deviceInfo builder
     *
     * @param deviceName
     * @param portAvailable
     * @param resolution
     * @return
     */
    public static Data.DeviceInfo.Builder genDeviceInfo(String deviceName, int portAvailable, Data.Resolution.Builder resolution) {
        return genDeviceInfo(ByteStringUtils.stringToByteString(deviceName), portAvailable, resolution);
    }


    /**
     * 生成完整的file request
     *
     * @param type
     * @param filePath
     * @return
     */
    public static Data.FileRequest.Builder genFileRequest(Data.FileRequest.FileRequestType type, ByteString filePath) {
        return Data.FileRequest.newBuilder().setFilePath(filePath).setFileRequestType(type);
    }


    /**
     * 生成不带icon的file info
     *
     * @param filePath
     * @param fileSize
     * @return
     */
    public static Data.FileInfo.Builder genFileInfo(ByteString filePath, long fileSize) {
        return Data.FileInfo.newBuilder().setFileSize(fileSize).setFilePath(filePath);
    }

    /**
     * 生成完整的fileInfo builder
     *
     * @param filePath
     * @param fileSize
     * @param fileIcon
     * @return
     */
    public static Data.FileInfo.Builder genFileInfo(ByteString filePath, long fileSize, ByteString fileIcon) {
        return genFileInfo(filePath, fileSize).setFileIcon(fileIcon);
    }


    /**
     * 生成完整的file block
     *
     * @param content
     * @return
     */
    public static Data.FileBlock.Builder genFileBlock(ByteString content) {
        return Data.FileBlock.newBuilder().setContent(content);
    }


    /**
     * 默认过期时间的token request
     *
     * @return
     */
    public static Data.CloudStorageTokenRequest.Builder genCloudStorageTokenRequest() {
        return Data.CloudStorageTokenRequest.newBuilder();
    }

    /**
     * 带有过期时间的token request
     *
     * @param expires 单位为秒
     * @return
     */
    public static Data.CloudStorageTokenRequest.Builder genCloudStorageTokenRequest(int expires) {
        return genCloudStorageTokenRequest().setExpires(expires);
    }

    /**
     * 生成token
     *
     * @param token
     * @return
     */
    public static Data.CloudStorageToken.Builder genCloudStorageToken(ByteString token) {
        return Data.CloudStorageToken.newBuilder().setToken(token);
    }

    /**
     * 生成message
     *
     * @param message
     * @return
     */
    public static Data.SharedMessage.Builder genSharedMessage(ByteString message) {
        return Data.SharedMessage.newBuilder().setContent(message);
    }

    /**
     * 生成keyboard event
     *
     * @param keyCode
     * @return
     */
    public static Data.KeyboardEvent.Builder genKeyboardEvent(int keyCode) {
        return Data.KeyboardEvent.newBuilder().setKeyCode(keyCode);
    }
}
