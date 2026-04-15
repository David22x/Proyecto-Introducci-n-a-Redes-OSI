package OSI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Receptor: recibe los bits transmitidos y los desencapsula
 * recorriendo las capas OSI de abajo (capa 1) hacia arriba (capa 7).
 *
 * La logica de visualizacion se delega al Visualizador,
 * manteniendo la separacion de responsabilidades.
 */
public class Receptor {

    private final List<Capa> capas = new ArrayList<>();

    // Descripciones del proceso real realizado en cada capa
    private static final String[] PROCESOS_DESENCAPSULACION = {
            "Convierte bits a texto: reconstruye caracteres desde binario",
            "Verifica CRC-16: confirma integridad de la trama recibida",
            "Quita cabecera IP y ruta de enrutamiento",
            "Reensambla segmentos numerados respetando orden de secuencia",
            "Cierra sesion: retira ID, hora y checkpoint de control",
            "Descomprime datos DEFLATE+Base64 y recupera texto original",
            "Quita cabecera HTTP y entrega mensaje final al usuario"
    };

    public Receptor() {
        capas.add(new CapaAplicacion());
        capas.add(new CapaPresentacion());
        capas.add(new CapaSesion());
        capas.add(new CapaTransporte());
        capas.add(new CapaRed());
        capas.add(new CapaEnlace());
        capas.add(new CapaFisica());

        // Invertimos para procesar de capa 1 a capa 7
        Collections.reverse(capas);
    }

    /**
     * Desencapsula el PDU recibido pasando por cada capa de abajo a arriba.
     *
     * @param pduRecibida La PDU con los bits transmitidos por el emisor
     * @return El mensaje original del usuario
     */
    public String recibir(PDU pduRecibida) {
        Visualizador.mostrarInicioRecepcion(pduRecibida.getDatos());

        PDU pdu = pduRecibida;

        for (int i = 0; i < capas.size(); i++) {
            Capa capa = capas.get(i);
            String entradaAntes = pdu.getDatos();
            pdu = capa.desencapsular(pdu);
            Visualizador.mostrarPasoDesencapsulacion(
                    capa.getNombre(),
                    entradaAntes,
                    pdu.getDatos(),
                    PROCESOS_DESENCAPSULACION[i]);
        }

        return pdu.getDatos();
    }
}
