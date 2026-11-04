package com.veterinaria.veterinaria.service;

// ==========================================================
// IMPORTACIONES: JAVA
// ==========================================================
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// ==========================================================
// IMPORTACIONES: APACHE PDFBOX 3
// ==========================================================
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

// ==========================================================
// IMPORTACIONES: SPRING Y PROYECTO
// ==========================================================
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.veterinaria.model.Factura.EstadoFactura;
import com.veterinaria.veterinaria.service.FacturaService.FacturaRespuesta;
import com.veterinaria.veterinaria.service.FacturaService.LineaMedicamentoFactura;

// ==========================================================
// PASO 63 - GENERACION DE PDF DE FACTURAS INTERNAS
// ==========================================================
// - NO modifica facturas, precios ni existencias.
// - Utiliza los datos ya autorizados por FacturaService.
// - ADMIN, VETERINARIO y USER pueden descargar solamente
//   las facturas a las que tienen acceso.
// - Si hay muchos medicamentos, crea nuevas paginas A4.
// - Es un comprobante INTERNO; NO es una factura SRI.
// - El envio SMTP se implementara en un paso posterior.
// ==========================================================
@Service
public class FacturaPdfService {

    private final FacturaService facturaService;

    public FacturaPdfService(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    // ======================================================
    // GENERAR UN PDF EN MEMORIA
    // ======================================================
    // Recibe el ID y el correo obtenido del JWT, nunca
    // una identidad suministrada por el cuerpo JSON.
    // El Service existente comprueba los permisos reales.
    // ======================================================
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','USER')")
    public byte[] generarPdf(Long facturaId, String correoAutenticado) {
        FacturaRespuesta factura = facturaService.buscarPorId(
                facturaId, correoAutenticado
        );

        if (factura.estado() != EstadoFactura.EMITIDA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La factura debe estar EMITIDA para descargar su PDF"
            );
        }

        try (CompositorPdf pdf = new CompositorPdf()) {
            pdf.escribirFactura(factura);
            return pdf.obtenerBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible generar el PDF de la factura",
                    ex
            );
        }
    }

    // ======================================================
    // CLASE AUXILIAR: DIBUJAR EL DOCUMENTO
    // ======================================================
    private static final class CompositorPdf implements AutoCloseable {

        // Fuentes integradas: no exige archivos .ttf externos.
        private static final PDFont NORMAL =
                new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private static final PDFont NEGRITA =
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        // Colores institucionales sencillos.
        private static final Color AZUL = new Color(20, 47, 68);
        private static final Color VERDE = new Color(15, 132, 112);
        private static final Color GRIS = new Color(93, 108, 116);
        private static final Color CLARO = new Color(243, 247, 249);
        private static final Color BORDE = new Color(222, 230, 233);
        private static final Color BLANCO = Color.WHITE;

        // Hoja A4: aproximadamente 595 x 842 puntos.
        private static final float ANCHO = PDRectangle.A4.getWidth();
        private static final float ALTO = PDRectangle.A4.getHeight();
        private static final float MARGEN = 40;
        private static final float ANCHO_UTIL = ANCHO - MARGEN * 2;
        private static final float LIMITE_INFERIOR = 115;

        private static final DateTimeFormatter FECHA =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        private final PDDocument documento = new PDDocument();
        private PDPageContentStream lienzo;
        private float y;
        private int numeroPagina = 0;
        private int numeroFila = 0;
        private String numeroComprobante = "";

        // --------------------------------------------------
        // ESTRUCTURA PRINCIPAL DE LA FACTURA
        // --------------------------------------------------
        void escribirFactura(FacturaRespuesta factura) throws IOException {
            numeroComprobante = valor(factura.numeroFactura());
            nuevaPagina(true, numeroComprobante, false);
            datosGenerales(factura);
            tituloSeccion("DETALLE DE SERVICIOS Y MEDICAMENTOS");
            cabeceraTabla();

            // Servicio: siempre se incluye primero.
            dibujarFila(
                    factura.nombreServicio(),
                    "Atencion veterinaria",
                    "SERVICIO",
                    "1",
                    moneda(factura.precioServicio()),
                    moneda(factura.precioServicio())
            );

            // Medicamentos: se muestran tanto los cobrados
            // como los solamente recetados (subtotal cero).
            if (factura.medicamentos() != null) {
                for (LineaMedicamentoFactura med : factura.medicamentos()) {
                    dibujarFila(
                            med.nombreMedicamento(),
                            med.indicaciones(),
                            tipoLegible(med),
                            cantidad(med),
                            moneda(med.precioUnitario()),
                            moneda(med.subtotal())
                    );
                }
            }

            totales(factura);
            avisoInterno();
        }

        // --------------------------------------------------
        // NUEVA PAGINA: ENCABEZADO Y PIE
        // --------------------------------------------------
        private void nuevaPagina(boolean primera, String numero, boolean repetirTabla)
                throws IOException {
            if (lienzo != null) {
                lienzo.close();
            }

            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            lienzo = new PDPageContentStream(documento, pagina);
            numeroPagina++;

            if (primera) {
                rectangulo(0, ALTO - 112, ANCHO, 112, AZUL);
                texto("VETERINARIA", 43, ALTO - 58, NEGRITA, 23, BLANCO);
                texto("Comprobante de atencion medica", 44, ALTO - 80,
                        NORMAL, 10, new Color(214, 231, 234));
                textoDerecha("COMPROBANTE INTERNO", ANCHO - 41,
                        ALTO - 56, NEGRITA, 10, BLANCO);
                textoDerecha("No. " + valor(numero), ANCHO - 41,
                        ALTO - 77, NORMAL, 9, BLANCO);
                y = ALTO - 135;
            } else {
                rectangulo(0, ALTO - 72, ANCHO, 72, AZUL);
                texto("VETERINARIA", MARGEN, ALTO - 43,
                        NEGRITA, 16, BLANCO);
                textoDerecha("CONTINUACION - " + valor(numero),
                        ANCHO - MARGEN, ALTO - 42, NORMAL, 9, BLANCO);
                y = ALTO - 91;
                if (repetirTabla) {
                    cabeceraTabla();
                }
            }

            // Pie discreto incluido en todas las paginas.
            linea(MARGEN, 57, ANCHO - MARGEN, 57, BORDE, 0.8f);
            texto("Documento interno del sistema veterinario - No autorizado por el SRI",
                    MARGEN, 41, NORMAL, 7.5f, GRIS);
            textoDerecha("Pagina " + numeroPagina, ANCHO - MARGEN, 41,
                    NORMAL, 7.5f, GRIS);
        }

        // --------------------------------------------------
        // DATOS DEL PROPIETARIO Y DE LA ATENCION
        // --------------------------------------------------
        private void datosGenerales(FacturaRespuesta f) throws IOException {
            tituloSeccion("INFORMACION GENERAL");

            dosColumnas("PROPIETARIO", f.nombreCliente(),
                    "MASCOTA", f.nombreMascota());
            dosColumnas("CORREO DEL PROPIETARIO", f.correoCliente(),
                    "VETERINARIO", f.nombreVeterinario());
            dosColumnas("NUMERO INTERNO", f.numeroFactura(),
                    "FECHA DE EMISION", fecha(f.fechaEmision()));
            dosColumnas("ATENCION MEDICA", "#" + f.atencionId(),
                    "CITA", "#" + f.citaId());
            y -= 14;
        }

        private void dosColumnas(
                String etiquetaA, String valorA,
                String etiquetaB, String valorB
        ) throws IOException {
            float columna = (ANCHO_UTIL - 16) / 2;
            List<String> a = dividir(valorA, NORMAL, 10, columna - 9);
            List<String> b = dividir(valorB, NORMAL, 10, columna - 9);
            int n = Math.max(a.size(), b.size());
            float altura = 22 + Math.max(1, n) * 12 + 10;

            asegurarEspacio(altura + 12, false);
            texto(etiquetaA, MARGEN + 5, y - 8, NEGRITA, 8, GRIS);
            texto(etiquetaB, MARGEN + columna + 21,
                    y - 8, NEGRITA, 8, GRIS);

            for (int i = 0; i < a.size(); i++) {
                texto(a.get(i), MARGEN + 5, y - 23 - i * 12,
                        NORMAL, 10, AZUL);
            }
            for (int i = 0; i < b.size(); i++) {
                texto(b.get(i), MARGEN + columna + 21,
                        y - 23 - i * 12, NORMAL, 10, AZUL);
            }
            y -= altura;
        }

        private void tituloSeccion(String titulo) throws IOException {
            asegurarEspacio(33, false);
            texto(titulo, MARGEN + 3, y, NEGRITA, 10, VERDE);
            y -= 25;
        }

        // --------------------------------------------------
        // CABECERA Y FILAS DE LA TABLA
        // --------------------------------------------------
        private void cabeceraTabla() throws IOException {
            rectangulo(MARGEN, y - 29, ANCHO_UTIL, 30, AZUL);
            texto("DESCRIPCION", MARGEN + 9, y - 19,
                    NEGRITA, 8, BLANCO);
            texto("TIPO", 290, y - 19, NEGRITA, 8, BLANCO);
            textoDerecha("CANT.", 435, y - 19,
                    NEGRITA, 8, BLANCO);
            textoDerecha("P. UNIT.", 493, y - 19,
                    NEGRITA, 8, BLANCO);
            textoDerecha("IMPORTE", ANCHO - MARGEN - 8,
                    y - 19, NEGRITA, 8, BLANCO);
            y -= 30;
        }

        private void dibujarFila(
                String descripcion,
                String indicaciones,
                String tipo,
                String cantidad,
                String precio,
                String importe
        ) throws IOException {
            List<String> descripcionLineas = dividir(
                    descripcion, NEGRITA, 8.5f, 225);
            List<String> indicacionesLineas = dividir(
                    indicaciones, NORMAL, 7.5f, 225);

            float alturaTexto = descripcionLineas.size() * 11;
            if (!indicacionesLineas.isEmpty()) {
                alturaTexto += indicacionesLineas.size() * 10 + 3;
            }
            float altura = Math.max(39, alturaTexto + 16);

            // Un texto excepcionalmente largo se divide por segmentos
            // para evitar que una sola fila salga fuera de una pagina.
            final float maxFila = ALTO - 220;
            if (altura > maxFila) {
                int lineasPermitidas = Math.max(1, (int) ((maxFila - 45) / 10));
                while (indicacionesLineas.size() > lineasPermitidas) {
                    List<String> segmento = new ArrayList<>(
                            indicacionesLineas.subList(0, lineasPermitidas));
                    indicacionesLineas = new ArrayList<>(
                            indicacionesLineas.subList(lineasPermitidas,
                                    indicacionesLineas.size()));
                    dibujarFilaSegmento(descripcionLineas, segmento,
                            tipo, cantidad, precio, "(continua)");
                    descripcionLineas = List.of("Continuacion de indicaciones");
                    tipo = "";
                    cantidad = "";
                    precio = "";
                }
                altura = Math.max(39, descripcionLineas.size() * 11
                        + indicacionesLineas.size() * 10 + 19);
            }
            dibujarFilaSegmento(descripcionLineas, indicacionesLineas,
                    tipo, cantidad, precio, importe);
        }

        private void dibujarFilaSegmento(
                List<String> descripcion,
                List<String> indicaciones,
                String tipo,
                String cantidad,
                String precio,
                String importe
        ) throws IOException {
            float altura = Math.max(39, descripcion.size() * 11
                    + (indicaciones.isEmpty() ? 0 : indicaciones.size() * 10 + 3)
                    + 16);
            asegurarEspacio(altura + 8, true);

            numeroFila++;
            if (numeroFila % 2 == 0) {
                rectangulo(MARGEN, y - altura, ANCHO_UTIL, altura, CLARO);
            }
            linea(MARGEN, y - altura, ANCHO - MARGEN,
                    y - altura, BORDE, 0.5f);

            float lineaY = y - 15;
            for (String s : descripcion) {
                texto(s, MARGEN + 9, lineaY, NEGRITA, 8.5f, AZUL);
                lineaY -= 11;
            }
            lineaY -= 2;
            for (String s : indicaciones) {
                texto(s, MARGEN + 9, lineaY, NORMAL, 7.5f, GRIS);
                lineaY -= 10;
            }

            texto(tipo, 290, y - 16, NORMAL, 7.5f, AZUL);
            textoDerecha(cantidad, 435, y - 16, NORMAL, 8, AZUL);
            textoDerecha(precio, 493, y - 16, NORMAL, 8, AZUL);
            textoDerecha(importe, ANCHO - MARGEN - 8,
                    y - 16, NEGRITA, 8, AZUL);
            y -= altura;
        }

        // --------------------------------------------------
        // TOTALES Y ADVERTENCIA TRIBUTARIA
        // --------------------------------------------------
        private void totales(FacturaRespuesta f) throws IOException {
            asegurarEspacio(158, false);
            y -= 18;

            float xEtiqueta = 349;
            float xImporte = ANCHO - MARGEN - 8;
            texto("Servicio", xEtiqueta, y, NORMAL, 9, GRIS);
            textoDerecha(moneda(f.precioServicio()), xImporte,
                    y, NORMAL, 9, AZUL);
            y -= 21;
            texto("Medicamentos", xEtiqueta, y, NORMAL, 9, GRIS);
            textoDerecha(moneda(f.totalMedicamentos()), xImporte,
                    y, NORMAL, 9, AZUL);
            y -= 21;
            texto("Subtotal", xEtiqueta, y, NORMAL, 9, GRIS);
            textoDerecha(moneda(f.subtotal()), xImporte,
                    y, NORMAL, 9, AZUL);
            y -= 21;
            texto("Impuestos*", xEtiqueta, y, NORMAL, 9, GRIS);
            textoDerecha(moneda(f.impuestos()), xImporte,
                    y, NORMAL, 9, AZUL);
            y -= 14;

            rectangulo(335, y - 39, ANCHO - MARGEN - 335, 41, VERDE);
            texto("TOTAL", 348, y - 24, NEGRITA, 12, BLANCO);
            textoDerecha(moneda(f.total()), xImporte,
                    y - 24, NEGRITA, 14, BLANCO);
            y -= 59;
        }

        private void avisoInterno() throws IOException {
            asegurarEspacio(72, false);
            texto("COMPROBANTE INTERNO - NO AUTORIZADO POR EL SRI",
                    MARGEN + 4, y, NEGRITA, 8.4f, AZUL);
            y -= 14;
            for (String s : dividir(
                    "* Los impuestos mostrados corresponden a la configuracion "
                    + "provisional de este proyecto academico. "
                    + "Este documento no sustituye una factura tributaria autorizada.",
                    NORMAL, 8, ANCHO_UTIL - 9)) {
                texto(s, MARGEN + 4, y, NORMAL, 8, GRIS);
                y -= 11;
            }
        }

        // --------------------------------------------------
        // SALTO DE PAGINA AUTOMATICO
        // --------------------------------------------------
        private void asegurarEspacio(float requerido, boolean repetirTabla)
                throws IOException {
            if (y - requerido >= LIMITE_INFERIOR) {
                return;
            }
            // Los saltos de pagina conservan el encabezado y la tabla.
            // El numero de comprobante se muestra en la primera pagina;
            // se mantiene una cabecera generica al continuar.
            nuevaPagina(false, numeroComprobante, repetirTabla);
        }

        // --------------------------------------------------
        // OPERACIONES GRAFICAS DE PDFBOX
        // --------------------------------------------------
        private void rectangulo(float x, float origenY, float ancho,
                               float alto, Color color) throws IOException {
            lienzo.setNonStrokingColor(color);
            lienzo.addRect(x, origenY, ancho, alto);
            lienzo.fill();
        }

        private void linea(float x1, float y1, float x2, float y2,
                           Color color, float grosor) throws IOException {
            lienzo.setStrokingColor(color);
            lienzo.setLineWidth(grosor);
            lienzo.moveTo(x1, y1);
            lienzo.lineTo(x2, y2);
            lienzo.stroke();
        }

        private void texto(String contenido, float x, float posicionY,
                           PDFont fuente, float tamano, Color color)
                throws IOException {
            String limpio = limpiar(contenido, fuente);
            if (limpio.isEmpty()) {
                return;
            }
            lienzo.setNonStrokingColor(color);
            lienzo.beginText();
            lienzo.setFont(fuente, tamano);
            lienzo.newLineAtOffset(x, posicionY);
            lienzo.showText(limpio);
            lienzo.endText();
        }

        private void textoDerecha(String contenido, float derecha,
                                  float posicionY, PDFont fuente,
                                  float tamano, Color color) throws IOException {
            String limpio = limpiar(contenido, fuente);
            float anchoTexto = fuente.getStringWidth(limpio) * tamano / 1000f;
            texto(limpio, derecha - anchoTexto, posicionY,
                    fuente, tamano, color);
        }

        private List<String> dividir(String contenido, PDFont fuente,
                                      float tamano, float anchoMaximo)
                throws IOException {
            String entrada = limpiar(contenido, fuente).trim();
            List<String> lineas = new ArrayList<>();
            if (entrada.isEmpty()) {
                return lineas;
            }
            String actual = "";
            for (String palabra : entrada.split("\\s+")) {
                String propuesta = actual.isEmpty() ? palabra : actual + " " + palabra;
                if (fuente.getStringWidth(propuesta) * tamano / 1000f
                        <= anchoMaximo) {
                    actual = propuesta;
                } else {
                    if (!actual.isEmpty()) {
                        lineas.add(actual);
                    }
                    actual = "";
                    // Romper palabras largas (ej. correos sin espacios).
                    for (int offset = 0; offset < palabra.length(); offset++) {
                        String prueba = actual + palabra.charAt(offset);
                        if (!actual.isEmpty()
                                && fuente.getStringWidth(prueba) * tamano / 1000f
                                > anchoMaximo) {
                            lineas.add(actual);
                            actual = "";
                        }
                        actual += palabra.charAt(offset);
                    }
                }
            }
            if (!actual.isEmpty()) {
                lineas.add(actual);
            }
            return lineas;
        }

        // La fuente Helvetica cubre los acentos espanoles habituales.
        // Un simbolo no soportado se reemplaza en vez de romper el PDF.
        private String limpiar(String contenido, PDFont fuente)
                throws IOException {
            if (contenido == null) {
                return "";
            }
            StringBuilder salida = new StringBuilder();
            String texto = contenido.replaceAll("[\\r\\n\\t]", " ");
            for (int i = 0; i < texto.length();) {
                int cp = texto.codePointAt(i);
                i += Character.charCount(cp);
                if (Character.isISOControl(cp)) {
                    continue;
                }
                String caracter = new String(Character.toChars(cp));
                try {
                    fuente.encode(caracter);
                    salida.append(caracter);
                } catch (IllegalArgumentException e) {
                    salida.append('?');
                }
            }
            return salida.toString();
        }

        // --------------------------------------------------
        // FORMATO DE VALORES DE NEGOCIO
        // --------------------------------------------------
        private static String moneda(BigDecimal monto) {
            DecimalFormatSymbols simbolos =
                    new DecimalFormatSymbols(Locale.forLanguageTag("es-EC"));
            simbolos.setDecimalSeparator(',');
            simbolos.setGroupingSeparator('.');
            DecimalFormat formato = new DecimalFormat("#,##0.00", simbolos);
            formato.setRoundingMode(RoundingMode.HALF_UP);
            return "$" + formato.format(monto == null ? BigDecimal.ZERO : monto);
        }

        private static String cantidad(LineaMedicamentoFactura med) {
            String numero = med.cantidad() == null ? "0"
                    : med.cantidad().stripTrailingZeros().toPlainString();
            return numero + " " + valor(med.unidadMedida());
        }

        private static String tipoLegible(LineaMedicamentoFactura med) {
            if (med.tipoRegistro() == null) {
                return "MEDICAMENTO";
            }
            return switch (med.tipoRegistro()) {
                case UTILIZADO -> "UTILIZADO";
                case RECETADO -> "SOLO RECETADO";
                case RECETADO_ENTREGADO -> "ENTREGADO";
            };
        }

        private static String fecha(LocalDateTime fecha) {
            return fecha == null ? "Sin fecha" : fecha.format(FECHA);
        }

        private static String valor(String valor) {
            return valor == null ? "" : valor;
        }

        // --------------------------------------------------
        // PDF EN BYTES: UTIL PARA DESCARGA Y CORREO
        // --------------------------------------------------
        byte[] obtenerBytes() throws IOException {
            if (lienzo != null) {
                lienzo.close();
                lienzo = null;
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }

        @Override
        public void close() throws IOException {
            if (lienzo != null) {
                lienzo.close();
                lienzo = null;
            }
            documento.close();
        }
    }
}
