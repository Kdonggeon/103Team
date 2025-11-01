package com.team103.dto;

import java.util.List;

public class RoomVectorLayoutResponse {
    private Integer version;
    private Double canvasW;
    private Double canvasH;
    private List<VectorSeat> seats;

    public static class VectorSeat {
        private String id;
        private String label;
        private Double x, y, w, h;
        private Double r;
        private Boolean disabled;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Double getX() { return x; }
        public void setX(Double x) { this.x = x; }
        public Double getY() { return y; }
        public void setY(Double y) { this.y = y; }
        public Double getW() { return w; }
        public void setW(Double w) { this.w = w; }
        public Double getH() { return h; }
        public void setH(Double h) { this.h = h; }
        public Double getR() { return r; }
        public void setR(Double r) { this.r = r; }
        public Boolean getDisabled() { return disabled; }
        public void setDisabled(Boolean disabled) { this.disabled = disabled; }
    }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Double getCanvasW() { return canvasW; }
    public void setCanvasW(Double canvasW) { this.canvasW = canvasW; }
    public Double getCanvasH() { return canvasH; }
    public void setCanvasH(Double canvasH) { this.canvasH = canvasH; }
    public List<VectorSeat> getSeats() { return seats; }
    public void setSeats(List<VectorSeat> seats) { this.seats = seats; }
}
