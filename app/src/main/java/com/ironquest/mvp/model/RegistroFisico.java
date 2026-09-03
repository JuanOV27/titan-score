package com.ironquest.mvp.model;

/** Una toma de medidas corporales en una fecha concreta. */
public class RegistroFisico extends EntidadIdentificable {

    private String fecha;
    private double alturaCm;
    private double pesoKg;
    private double imc;
    private double pechoCm;
    private double cinturaCm;
    private double caderaCm;
    private double brazoCm;
    private double piernaCm;
    private String tipoCuerpo;

    /** Público: el formulario de medidas construye el registro vacío y lo va llenando. */
    public RegistroFisico() {
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getAlturaCm() {
        return alturaCm;
    }

    public void setAlturaCm(double alturaCm) {
        this.alturaCm = alturaCm;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public double getImc() {
        return imc;
    }

    public void setImc(double imc) {
        this.imc = imc;
    }

    public double getPechoCm() {
        return pechoCm;
    }

    public void setPechoCm(double pechoCm) {
        this.pechoCm = pechoCm;
    }

    public double getCinturaCm() {
        return cinturaCm;
    }

    public void setCinturaCm(double cinturaCm) {
        this.cinturaCm = cinturaCm;
    }

    public double getCaderaCm() {
        return caderaCm;
    }

    public void setCaderaCm(double caderaCm) {
        this.caderaCm = caderaCm;
    }

    public double getBrazoCm() {
        return brazoCm;
    }

    public void setBrazoCm(double brazoCm) {
        this.brazoCm = brazoCm;
    }

    public double getPiernaCm() {
        return piernaCm;
    }

    public void setPiernaCm(double piernaCm) {
        this.piernaCm = piernaCm;
    }

    public String getTipoCuerpo() {
        return tipoCuerpo;
    }

    public void setTipoCuerpo(String tipoCuerpo) {
        this.tipoCuerpo = tipoCuerpo;
    }
}
