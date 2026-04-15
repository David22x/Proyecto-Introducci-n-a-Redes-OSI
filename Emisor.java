package OSI;

import java.util.ArrayList;
import java.util.List;

/**
 * Emisor: recibe datos del usuario y los encapsula
 * recorriendo las capas OSI de arriba (capa 7) hacia abajo (capa 1).
 *
 * La logica de visualizacion se delega al Visualizador,
 * manteniendo la separacion de responsabilidades.
 */
public class Emisor {

    private final List<Capa> capas = new ArrayList<>();

    // Descripciones del proceso real realizado en cada capa
    private static final String[] PROCESOS_ENCAPSULACION = {
            "Define reglas HTTP: metodo GET, recurso, host y longitud",
            "Comprime datos con DEFLATE+Base64 para optimizar transmision",
            "Abre sesion unica, asigna ID, hora y checkpoint de control",
            "Segmenta en ventanas de " + CapaTransporte.TAMANIO_SEGMENTO + " bytes para regular el flujo",
            "Enruta el paquete: calcula ruta por routers y decrementa TTL",
            "Garantiza confiabilidad: adjunta CRC-16 para deteccion de errores",
            "Convierte todo a bits binarios para transmitir por el medio fisico"
    };

    public Emisor() {
        capas.add(new CapaAplicacion());
        capas.add(new CapaPresentacion());
        capas.add(new CapaSesion());
        capas.add(new CapaTransporte());
        capas.add(new CapaRed());
        capas.add(new CapaEnlace());
        capas.add(new CapaFisica());
    }

    /**
     * Encapsula el mensaje pasando por cada capa de arriba a abajo.
     *
     * @param mensaje El texto ingresado por el usuario
     * @return PDU final en forma de bits, lista para "transmitir"
     */
    public PDU enviar(String mensaje) {
        Visualizador.mostrarInicioEnvio(mensaje);

        PDU pdu = new PDU(mensaje);

        for (int i = 0; i < capas.size(); i++) {
            Capa capa = capas.get(i);
            String entradaAntes = pdu.getDatos();
            pdu = capa.encapsular(pdu);
            Visualizador.mostrarPasoEncapsulacion(
                    capa.getNombre(),
                    entradaAntes,
                    pdu.getDatos(),
                    PROCESOS_ENCAPSULACION[i]);
        }

        return pdu;
    }
}
