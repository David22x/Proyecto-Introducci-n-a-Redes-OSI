package OSI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Receptor: recibe los bits transmitidos y los desencapsula
 * recorriendo las capas OSI de abajo (capa 1) hacia arriba (capa 7).
 *
 * La lógica de visualización se delega al Visualizador,
 * manteniendo la separación de responsabilidades.
 */
public class Receptor {

    private final List<Capa> capas = new ArrayList<>();

    // Descripciones del proceso realizado en cada capa (para visualización)
    private static final String[] PROCESOS_DESENCAPSULACION = {
            "Convierte bits a texto (reconstruccion de caracteres)",
            "Quita cabecera MAC/CRC",
            "Quita cabecera IP",
            "Reensambla segmentos numerados en orden",
            "Quita cabecera de sesion",
            "Descifra contenido Cesar-3, quita cabecera de codificacion",
            "Quita cabecera HTTP; entrega mensaje al usuario"
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