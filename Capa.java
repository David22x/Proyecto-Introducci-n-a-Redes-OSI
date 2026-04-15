package OSI;

import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.Base64;

/**
 * Interfaz común para todas las capas del modelo OSI.
 * Define el contrato de encapsulación y desencapsulación.
 */
public interface Capa {

    PDU encapsular(PDU pdu);

    /**
     * Implementación por defecto: elimina la cabecera separada por " | ".
     * Las capas con lógica especial sobreescriben este método.
     */
    default PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();
        int separador = contenido.indexOf(" | ");
        if (separador != -1) {
            contenido = contenido.substring(separador + 3);
        }
        return new PDU(contenido);
    }

    String getNombre();
}


// ============================================================
// CAPA 7 - Aplicacion
//
// FUNCION: Define las reglas que permiten la comunicacion.
// En el modelo OSI real, esta capa establece el protocolo
// que van a usar emisor y receptor para entenderse
// (HTTP, FTP, SMTP, etc.), incluyendo el metodo de la
// peticion, la version del protocolo y el recurso solicitado.
//
// IMPLEMENTACION: Se agrega una cabecera HTTP completa que
// simula una peticion GET real, definiendo las reglas de
// comunicacion entre cliente y servidor.
// PDU resultante = MENSAJE
// ============================================================
class CapaAplicacion implements Capa {

    private static final String PROTOCOLO = "HTTP";
    private static final String VERSION   = "1.1";
    private static final String METODO    = "GET";
    private static final String RECURSO   = "/index.html";
    private static final String HOST      = "servidor.uta.edu.ec";

    @Override
    public PDU encapsular(PDU pdu) {
        // Cabecera HTTP que define las reglas de comunicacion:
        // protocolo/version acordados, metodo, recurso, host y longitud
        String cabecera = String.format(
            "[AH: protocolo=%s/%s, metodo=%s, recurso=%s, host=%s, content-length=%d]",
            PROTOCOLO, VERSION, METODO, RECURSO, HOST, pdu.getDatos().length()
        );
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    @Override
    public String getNombre() {
        return "Capa 7 - Aplicacion";
    }
}


// ============================================================
// CAPA 6 - Presentacion
//
// FUNCION: Reduce el tamano de los datos para optimizar
// su transmision (compresion).
// En el modelo OSI real, esta capa transforma el formato
// de los datos: compresion, cifrado y codificacion.
// La compresion reduce la cantidad de bytes a transmitir,
// optimizando el uso del ancho de banda.
//
// IMPLEMENTACION: Se comprime con el algoritmo DEFLATE
// (el mismo que usa ZIP/GZIP) y se codifica en Base64
// para representarlo como texto. Se reporta el porcentaje
// de reduccion obtenido.
// PDU resultante = DATO COMPRIMIDO
// ============================================================
class CapaPresentacion implements Capa {

    @Override
    public PDU encapsular(PDU pdu) {
        String datosOriginales = pdu.getDatos();
        int tamanoOriginal = datosOriginales.length();

        String comprimido = comprimir(datosOriginales);
        int tamanoComprimido = comprimido.length();

        int reduccion = (int)(((double)(tamanoOriginal - tamanoComprimido) / tamanoOriginal) * 100);
        String estado = reduccion > 0 ? "reduccion=" + reduccion + "%" : "sin-reduccion";

        String cabecera = String.format(
            "[PH: codificacion=UTF-8, compresion=DEFLATE, tam_original=%d, tam_comprimido=%d, %s]",
            tamanoOriginal, tamanoComprimido, estado
        );
        return new PDU(cabecera + " | " + comprimido);
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();
        int sep = contenido.indexOf(" | ");
        if (sep != -1) {
            contenido = contenido.substring(sep + 3);
        }
        return new PDU(descomprimir(contenido));
    }

    /** Comprime con DEFLATE y codifica en Base64. */
    private String comprimir(String texto) {
        try {
            byte[] input = texto.getBytes("UTF-8");
            Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.setInput(input);
            deflater.finish();
            byte[] output = new byte[input.length * 2];
            int len = deflater.deflate(output);
            deflater.end();
            byte[] resultado = new byte[len];
            System.arraycopy(output, 0, resultado, 0, len);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            return texto;
        }
    }

    /** Decodifica Base64 y descomprime con DEFLATE. */
    private String descomprimir(String comprimido) {
        try {
            byte[] input = Base64.getDecoder().decode(comprimido);
            Inflater inflater = new Inflater();
            inflater.setInput(input);
            byte[] output = new byte[input.length * 10];
            int len = inflater.inflate(output);
            inflater.end();
            return new String(output, 0, len, "UTF-8");
        } catch (Exception e) {
            return comprimido;
        }
    }

    @Override
    public String getNombre() {
        return "Capa 6 - Presentacion";
    }
}


// ============================================================
// CAPA 5 - Sesion
//
// FUNCION: Controla como se lleva a cabo la comunicacion.
// En el modelo OSI real, esta capa gestiona el dialogo:
// abre la sesion, la mantiene activa mediante checkpoints
// (puntos de control que permiten reanudar si se interrumpe)
// y la cierra ordenadamente al terminar.
//
// IMPLEMENTACION: Se asigna un ID de sesion unico, se
// registra la hora de inicio, se define el modo simplex
// (solo un sentido) y se numera un checkpoint por cada
// PDU procesada para simular puntos de reanudacion.
// PDU resultante = DATO CON CONTROL DE SESION
// ============================================================
class CapaSesion implements Capa {

    private static int contadorSesiones = 0;
    private final String sesionId;
    private final String horaInicio;
    private int checkpoint = 0;

    public CapaSesion() {
        contadorSesiones++;
        this.sesionId  = String.format("SES-%03d", contadorSesiones);
        this.horaInicio = new java.text.SimpleDateFormat("HH:mm:ss")
                              .format(new java.util.Date());
    }

    @Override
    public PDU encapsular(PDU pdu) {
        checkpoint++;
        // La cabecera de sesion controla:
        // ID unico, hora de inicio, modo de dialogo,
        // checkpoint de reanudacion y estado del ciclo de vida
        String cabecera = String.format(
            "[SH: sesion_id=%s, inicio=%s, modo=simplex, checkpoint=%d, estado=ABIERTA]",
            sesionId, horaInicio, checkpoint
        );
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    @Override
    public String getNombre() {
        return "Capa 5 - Sesion";
    }
}


// ============================================================
// CAPA 4 - Transporte
//
// FUNCION: Regula la cantidad de datos que se transmiten
// (control de flujo).
// En el modelo OSI real, TCP usa una ventana deslizante
// que limita cuantos bytes puede enviar el emisor antes
// de recibir confirmacion del receptor. Esto evita que el
// emisor sature al receptor enviando mas datos de los que
// puede procesar.
//
// IMPLEMENTACION: Se segmenta el mensaje en bloques del
// tamano de la ventana (TAMANIO_VENTANA), simulando el
// limite de flujo. Cada segmento lleva su numero de
// secuencia y el tamano de ventana disponible, tal como
// lo hace TCP en la realidad.
// PDU resultante = SEGMENTOS CON CONTROL DE FLUJO
// ============================================================
class CapaTransporte implements Capa {

    // Tamano de ventana: maximo de bytes enviables antes de confirmacion
    public static final int TAMANIO_VENTANA = 50;

    @Override
    public PDU encapsular(PDU pdu) {
        String datos = pdu.getDatos();
        StringBuilder resultado = new StringBuilder();
        int totalSegmentos = (int) Math.ceil((double) datos.length() / TAMANIO_VENTANA);

        for (int i = 0; i < totalSegmentos; i++) {
            int inicio    = i * TAMANIO_VENTANA;
            int fin       = Math.min(inicio + TAMANIO_VENTANA, datos.length());
            String fragmento = datos.substring(inicio, fin);

            // La cabecera incluye puertos, numero de secuencia,
            // total de segmentos y tamano de ventana (control de flujo)
            String cabecera = String.format(
                "[TH: proto=TCP, src_port=1234, dst_port=80, seq=%04d, total=%04d, ventana=%d_bytes]",
                i + 1, totalSegmentos, TAMANIO_VENTANA
            );
            resultado.append(cabecera).append(" | ").append(fragmento);
            if (i < totalSegmentos - 1) resultado.append(" §§ ");
        }

        return new PDU(resultado.toString());
    }

    @Override
    public PDU desencapsular(PDU pdu) {
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
// CAPA 3 - Red
//
// FUNCION: Enrutar paquetes desde el origen hasta el destino.
// En el modelo OSI real, esta capa determina el mejor camino
// a traves de multiples routers. Cada router decrementa el
// TTL (Time To Live) en 1; si llega a 0, el paquete se
// descarta para evitar que circule indefinidamente.
//
// IMPLEMENTACION: Se simula una tabla de enrutamiento con
// saltos intermedios (hops). Se calcula la ruta completa
// y el TTL resultante despues de todos los saltos.
// PDU resultante = PAQUETE ENRUTADO
// ============================================================
class CapaRed implements Capa {

    // Tabla de enrutamiento: destino -> [saltos intermedios]
    private static final String[][] TABLA = {
        {"200.10.5.3", "192.168.1.1", "10.0.0.1", "200.10.5.3"}
    };

    private static final String IP_ORIGEN  = "192.168.1.10";
    private static final String IP_DESTINO = "200.10.5.3";
    private static final int    TTL_INICIAL = 64;

    @Override
    public PDU encapsular(PDU pdu) {
        String ruta    = calcularRuta(IP_DESTINO);
        int    ttlFinal = calcularTTL(IP_DESTINO);

        // La cabecera incluye IPs (direccionamiento logico),
        // TTL restante, protocolo transportado y ruta seguida
        String cabecera = String.format(
            "[NH: ip_origen=%s, ip_destino=%s, TTL=%d, proto=TCP, ruta=%s]",
            IP_ORIGEN, IP_DESTINO, ttlFinal, ruta
        );
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    private String calcularRuta(String destino) {
        for (String[] entrada : TABLA) {
            if (entrada[0].equals(destino)) {
                StringBuilder ruta = new StringBuilder();
                for (int i = 0; i < entrada.length; i++) {
                    ruta.append(entrada[i]);
                    if (i < entrada.length - 1) ruta.append("->");
                }
                return ruta.toString();
            }
        }
        return IP_ORIGEN + "->" + destino;
    }

    private int calcularTTL(String destino) {
        for (String[] entrada : TABLA) {
            if (entrada[0].equals(destino)) {
                return TTL_INICIAL - (entrada.length - 1);
            }
        }
        return TTL_INICIAL - 1;
    }

    @Override
    public String getNombre() {
        return "Capa 3 - Red";
    }
}


// ============================================================
// CAPA 2 - Enlace de datos
//
// FUNCION: Garantizar la confiabilidad de la informacion.
// En el modelo OSI real, esta capa detecta errores mediante
// CRC (Cyclic Redundancy Check). El emisor calcula el CRC
// y lo adjunta. El receptor lo recalcula: si coincide, la
// trama llego correcta; si no, fue corrompida y se solicita
// retransmision.
//
// IMPLEMENTACION: Se calcula un CRC-16 real usando el
// polinomio estandar 0x8005 (el mismo de Ethernet y USB).
// Al desencapsular se verifica que el CRC coincida para
// confirmar la integridad de la trama.
// PDU resultante = TRAMA VERIFICADA
// ============================================================
class CapaEnlace implements Capa {

    @Override
    public PDU encapsular(PDU pdu) {
        int crc = calcularCRC16(pdu.getDatos());
        // Cabecera con MACs (direccionamiento fisico local)
        // y CRC calculado para que el receptor verifique integridad
        String cabecera = String.format(
            "[DH: mac_origen=AA:BB:CC:DD:EE:01, mac_destino=AA:BB:CC:DD:EE:02, CRC16=0x%04X, integridad=OK]",
            crc
        );
        return new PDU(cabecera + " | " + pdu.getDatos());
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();
        int crcRecibido  = extraerCRC(contenido);

        int sep = contenido.indexOf(" | ");
        String datos = (sep != -1) ? contenido.substring(sep + 3) : contenido;

        // Verificacion de integridad: recalculamos el CRC
        int crcCalculado = calcularCRC16(datos);
        if (crcRecibido != -1 && crcRecibido != crcCalculado) {
            System.out.println("  [!] ADVERTENCIA: CRC no coincide. Trama posiblemente corrompida.");
        }

        return new PDU(datos);
    }

    /** CRC-16 con polinomio 0x8005 (estandar Ethernet/USB). */
    public int calcularCRC16(String datos) {
        int crc = 0xFFFF;
        for (char c : datos.toCharArray()) {
            crc ^= (c << 8);
            for (int i = 0; i < 8; i++) {
                crc = ((crc & 0x8000) != 0) ? (crc << 1) ^ 0x8005 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return crc;
    }

    private int extraerCRC(String cabecera) {
        try {
            int idx = cabecera.indexOf("CRC16=0x");
            if (idx == -1) return -1;
            return Integer.parseInt(cabecera.substring(idx + 8, idx + 12), 16);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public String getNombre() {
        return "Capa 2 - Enlace de datos";
    }
}


// ============================================================
// CAPA 1 - Fisica
//
// FUNCION: Convertir los datos a bits para transmitirlos
// por el medio fisico (cable, fibra optica, aire).
// En el modelo OSI real, esta capa trabaja con senales
// electricas, opticas o electromagneticas. No entiende de
// bytes ni caracteres: solo de 0 y 1.
//
// IMPLEMENTACION: Se convierte cada byte del mensaje a su
// representacion binaria de 8 bits. Se informa la cantidad
// total de bits generados y la velocidad de transmision
// simulada, como hace una interfaz de red real.
// PDU resultante = FLUJO DE BITS
// ============================================================
class CapaFisica implements Capa {

    private static final int VELOCIDAD_MBPS = 100;

    @Override
    public PDU encapsular(PDU pdu) {
        StringBuilder bits = new StringBuilder();
        byte[] bytes;
        try {
            bytes = pdu.getDatos().getBytes("UTF-8");
        } catch (Exception e) {
            bytes = pdu.getDatos().getBytes();
        }

        // Convertimos cada byte a 8 bits binarios
        for (byte b : bytes) {
            bits.append(String.format("%8s", Integer.toBinaryString(b & 0xFF))
                            .replace(' ', '0'));
            bits.append(" ");
        }

        int totalBits = bytes.length * 8;

        // Cabecera fisica: total de bits, velocidad del medio
        // y codificacion de linea NRZ (Non-Return-to-Zero, estandar Ethernet)
        String cabecera = String.format(
            "[FISICO: total_bits=%d, velocidad=%dMbps, codificacion=NRZ]",
            totalBits, VELOCIDAD_MBPS
        );

        return new PDU(cabecera + " | [BITS: " + bits.toString().trim() + "]");
    }

    @Override
    public PDU desencapsular(PDU pdu) {
        String contenido = pdu.getDatos();

        // Quitamos la cabecera fisica
        int sep = contenido.indexOf(" | ");
        if (sep != -1) {
            contenido = contenido.substring(sep + 3);
        }

        // Extraemos y convertimos los bits de vuelta a texto
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
