package OSI;

import java.util.ArrayList;
import java.util.List;

/**
 * PDU (Protocol Data Unit): unidad de datos intercambiada entre capas.
 * Puede contener uno o múltiples segmentos cuando la capa de Transporte
 * realiza la segmentación del mensaje.
 */
public class PDU {
    private String datos;
    private List<String> segmentos; // Para representar segmentos en capa Transporte

    public PDU(String datos) {
        this.datos = datos;
        this.segmentos = new ArrayList<>();
    }

    public String getDatos() {
        return datos;
    }

    public void setDatos(String datos) {
        this.datos = datos;
    }

    public List<String> getSegmentos() {
        return segmentos;
    }

    public void setSegmentos(List<String> segmentos) {
        this.segmentos = segmentos;
    }

    @Override
    public String toString() {
        return datos;
    }
}