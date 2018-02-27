package xin.heipichao.qiaojilive.data.bean;

public class LiveRoomSettings {
    private String lineName;
    private String resolutionName;
    private int playerType;

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public String getResolutionName() {
        return resolutionName;
    }

    public void setResolutionName(String resolutionName) {
        this.resolutionName = resolutionName;
    }

    public int getPlayerType() {
        return playerType;
    }

    public void setPlayerType(int playerType) {
        this.playerType = playerType;
    }

    public static LiveRoomSettings create(String name){
        // TODO need use cache
        return new LiveRoomSettings();
    }
}
