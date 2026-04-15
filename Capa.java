package OSI;

/**
 * Interfaz común para todas las capas del modelo OSI.
 * Define el contrato de encapsulación y desencapsulación.
 */
public interface Capa {

    /**
     * Encapsula los datos añadiendo la cabecera de esta capa.
     * Simula el proceso de ENVÍO (de arriba hacia abajo en el modelo OSI).
     */
    PDU encapsular(PDU pdu);

    /**
     * Desencapsula los datos quitando la cabecera de esta capa.
     * Simula el proceso de RECEPCIÓN (de abajo hacia arriba en el modelo OSI).
     *
     * Implementación por defecto: elimina el primer segmento de cabecera
     * separado por " | ". Las capas con lógica especial (ej. Física, Transporte)
     * deben sobreescribir este método.
     */
    default PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();
        int separador = contenido.indexOf(" | ");
        if (separador != -1) {
            contenido = contenido.substring(separador + 3);
        }
        return new PDU(contenido);
    }

    /** Nombre descriptivo de la capa (ej. "Capa 7 - Aplicación"). */
    String getNombre();
}

// ============================================================
// CAPA 7 — Aplicación
// Genera el mensaje de usuario con protocolo de aplicación.
// PDU resultante = MENSAJE
// ============================================================
class CapaAplicacion implements Capa {
    @Override
    public PDU encapsular(PDU pdu) {
        String cabecera = "[AH: protocolo=HTTP, version=1.1]";
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    // Usa desencapsular() por defecto de la interfaz

    @Override
    public String getNombre() {
        return "Capa 7 - Aplicacion";
    }
}

// ============================================================
// CAPA 6 — Presentación
// Aplica un cifrado César simple para simular cifrado real.
// PDU resultante = DATO CIFRADO
// ============================================================
class CapaPresentacion implements Capa {
    private static final int DESPLAZAMIENTO = 3; // Cifrado César

    @Override
    public PDU encapsular(PDU pdu) {
        String cifrado = cifrarCesar(pdu.getDatos(), DESPLAZAMIENTO);
        String cabecera = "[PH: codificacion=UTF-8, cifrado=CESAR-3]";
        return new PDU(cabecera + " | " + cifrado);
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        // Primero quitamos la cabecera (comportamiento por defecto)
        String contenido = pdu.getDatos();
        int sep = contenido.indexOf(" | ");
        if (sep != -1) {
            contenido = contenido.substring(sep + 3);
        }
        // Luego desciframos
        return new PDU(cifrarCesar(contenido, -DESPLAZAMIENTO));
    }

    /** Cifrado César: desplaza cada carácter alfabético n posiciones. */
    private String cifrarCesar(String texto, int n) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                sb.append((char) ((c - base + n + 26) % 26 + base));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public String getNombre() {
        return "Capa 6 - Presentacion";
    }
}

// ============================================================
// CAPA 5 — Sesión
// Gestiona un identificador de sesión único por instancia.
// PDU resultante = DATO CON ID DE SESIÓN
// ============================================================
class CapaSesion implements Capa {
    private static int contadorSesiones = 0;
    private final String sesionId;

    public CapaSesion() {
        contadorSesiones++;
        this.sesionId = String.format("SES-%03d", contadorSesiones);
    }

    @Override
    public PDU encapsular(PDU pdu) {
        String cabecera = "[SH: sesion_id=" + sesionId + ", tipo=full-duplex]";
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    // Usa desencapsular() por defecto de la interfaz

    @Override
    public String getNombre() {
        return "Capa 5 - Sesion";
    }
}

// ============================================================
// CAPA 4 — Transporte
// Segmenta el mensaje en fragmentos de tamaño fijo y los numera.
// PDU resultante = SEGMENTOS NUMERADOS
// ============================================================
class CapaTransporte implements Capa {
    public static final int TAMANIO_SEGMENTO = 50; // caracteres por segmento

    @Override
    public PDU encapsular(PDU pdu) {
        String datos = pdu.getDatos();
        StringBuilder resultado = new StringBuilder();
        int totalSegmentos = (int) Math.ceil((double) datos.length() / TAMANIO_SEGMENTO);

        for (int i = 0; i < totalSegmentos; i++) {
            int inicio = i * TAMANIO_SEGMENTO;
            int fin = Math.min(inicio + TAMANIO_SEGMENTO, datos.length());
            String fragmento = datos.substring(inicio, fin);

            String cabecera = String.format(
                    "[TH: proto=TCP, src_port=1234, dst_port=80, seq=%04d, total=%04d]",
                    i + 1, totalSegmentos);
            resultado.append(cabecera).append(" | ").append(fragmento);
            if (i < totalSegmentos - 1)
                resultado.append(" §§ "); // separador entre segmentos
        }

        PDU salida = new PDU(resultado.toString());
        return salida;
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        // Dividimos por el separador de segmentos
        String[] segmentos = pdu.getDatos().split(" §§ ");
        StringBuilder reconstruido = new StringBuilder();

        for (String seg : segmentos) {
            int sep = seg.indexOf(" | ");
            if (sep != -1) {
                reconstruido.append(seg.substring(sep + 3));
            }
        }
        return new PDU(reconstruido.toString());
    }

    @Override
    public String getNombre() {
        return "Capa 4 - Transporte";
    }
}

// ============================================================
// CAPA 3 — Red
// Asigna direcciones lógicas (IP) y TTL. Simula enrutamiento.
// PDU resultante = PAQUETE
// ============================================================
class CapaRed implements Capa {
    @Override
    public PDU encapsular(PDU pdu) {
        String cabecera = "[NH: ip_origen=192.168.1.10, ip_destino=200.10.5.3, TTL=64, proto=TCP]";
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    // Usa desencapsular() por defecto de la interfaz

    @Override
    public String getNombre() {
        return "Capa 3 - Red";
    }
}

// ============================================================
// CAPA 2 — Enlace de datos
// Añade direcciones MAC y un CRC calculado (suma de bytes mod 65536).
// PDU resultante = TRAMA
// ============================================================
class CapaEnlace implements Capa {
    @Override
    public PDU encapsular(PDU pdu) {
        int crc = calcularCRC(pdu.getDatos());
        String cabecera = String.format(
                "[DH: mac_origen=AA:BB:CC:DD:EE:01, mac_destino=AA:BB:CC:DD:EE:02, CRC=0x%04X]", crc);
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    // Usa desencapsular() por defecto de la interfaz

    /** CRC simplificado: suma de valores ASCII módulo 65536. */
    private int calcularCRC(String datos) {
        int suma = 0;
        for (char c : datos.toCharArray())
            suma += c;
        return suma % 65536;
    }

    @Override
    public String getNombre() {
        return "Capa 2 - Enlace de datos";
    }
}

// ============================================================
// CAPA 1 — Física
// Convierte cada carácter a su representación binaria de 8 bits.
// PDU resultante = BITS
// ============================================================
class CapaFisica implements Capa {
    @Override
    public PDU encapsular(PDU pdu) {
        StringBuilder bits = new StringBuilder("[BITS: ");
        for (char c : pdu.getDatos().toCharArray()) {
            bits.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
            bits.append(" ");
        }
        bits.append("]");
        return new PDU(bits.toString());
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();
        if (contenido.startsWith("[BITS: ") && contenido.endsWith("]")) {
            contenido = contenido.substring(7, contenido.length() - 1).trim();
            String[] grupos = contenido.split(" ");
            StringBuilder texto = new StringBuilder();
            for (String grupo : grupos) {
                if (!grupo.isEmpty()) {
                    texto.append((char) Integer.parseInt(grupo, 2));
                }
            }
            return new PDU(texto.toString());
        }
        return pdu;
    }

    @Override
    public String getNombre() {
        return "Capa 1 - Fisica";
    }
}