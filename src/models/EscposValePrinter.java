package models;

import com.github.anastaciocintra.escpos.*;
import com.github.anastaciocintra.escpos.barcode.BarCode;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javax.print.PrintService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EscposValePrinter {

    public enum Barcode { CODE128, CODE39 }

    public static class Params {
        public String p_empresa;
        public String p_vale;
        public String p_nombres;
        public String p_depto;
        public String p_sede;
        public String p_fecha;
        public String p_cedula;
        public String p_barras1;

        public String impresora = "TIQUETES";
        public Barcode barcode = Barcode.CODE128;
        public boolean mostrarTextoBajoBarcode = false;
    }

    // private static final String SEP = "______________________________________________";
    private static final String SEP = "----------------------------------------------";
   
    public static void imprimir(Params p) throws Exception {

        PrintService ps = (p.impresora == null || p.impresora.trim().isEmpty())
                ? PrinterOutputStream.getDefaultPrintService()
                : PrinterOutputStream.getPrintServiceByName(p.impresora);

        if (ps == null) {
            String[] all = PrinterOutputStream.getListPrintServicesNames();
            StringBuilder sb = new StringBuilder("No se encontró la impresora: ").append(p.impresora).append("\nDisponibles:\n");
            for (String s : all) sb.append(" - ").append(s).append('\n');
            throw new IllegalStateException(sb.toString());
        }

        try (PrinterOutputStream pos = new PrinterOutputStream(ps);
             EscPos esc = new EscPos(pos)) {

            Style left   = new Style().setJustification(EscPosConst.Justification.Left_Default);
            Style center = new Style().setBold(true).setJustification(EscPosConst.Justification.Center);
            Style h1     = new Style(center).setBold(true).setFontSize(Style.FontSize._2, Style.FontSize._2);
                        
            //Style nombreXL = new Style(left).setBold(true).setFontSize(Style.FontSize._1, Style.FontSize._2);
            Style nombreXL = new Style(left).setBold(true).setFontSize(Style.FontSize._1, Style.FontSize._1);
           
            
            Style cedulaXL = new Style(left).setBold(true);

            String fecha = (p.p_fecha != null && !p.p_fecha.trim().isEmpty())
                    ? p.p_fecha
                    : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

            /* ENCABEZADO */
            esc.writeLF(h1, nv(p.p_empresa));

            String numeroCodigo = nz(p.p_barras1) ? p.p_barras1 : nv(p.p_cedula);
            if (nz(numeroCodigo)) {
                esc.writeLF(center,"Ticket: "+ numeroCodigo);
            }

            /* CUERPO */
            esc.writeLF(nombreXL,"  "+ nv(p.p_nombres));
            if (nz(p.p_cedula)) esc.writeLF(cedulaXL, "  " + p.p_cedula);
            if (nz(p.p_depto))  esc.writeLF(left,"  "+ p.p_depto);
            if (nz(p.p_sede))   esc.writeLF(left,"  "+ p.p_sede);

            //esc.writeLF(left,"  Fecha :  "+ fecha + "\n" + corteA80(SEP));
            esc.writeLF(left,"  Fecha : "+ fecha);
            esc.writeLF(center, corteA80(SEP));
            /* UTILIZACIÓN */
            String util = nv(p.p_vale);
            int idx = util.lastIndexOf('-');
            if (idx != -1 && idx + 1 < util.length()) {
                util = util.substring(idx + 1).trim();
            }
            if (!util.trim().isEmpty()) {
                esc.writeLF(center, "Utilización: " + util);
            }

            /* CÓDIGO DE BARRAS */
            String raw = nz(p.p_barras1) ? p.p_barras1 : nv(p.p_cedula);

            if (nz(raw)) {
                BarCode bc = new BarCode()
                        .setBarCodeSize(3, 80)
                        .setHRIPosition(BarCode.BarCodeHRIPosition.NotPrinted_Default)
                        .setJustification(EscPosConst.Justification.Center);

                if (p.barcode == Barcode.CODE39) {
                    bc.setSystem(BarCode.BarCodeSystem.CODE39_A);
                    esc.write(bc, raw);
                } else {
                    bc.setSystem(BarCode.BarCodeSystem.CODE128);
                    String data128 = raw.startsWith("{A") || raw.startsWith("{B") || raw.startsWith("{C")
                            ? raw
                            : "{B" + raw;
                    esc.write(bc, data128);
                }
            }

            /* PIE */
            esc.writeLF(center, "@Solutic3E | Transformando Ideas");
            esc.writeLF("");
            esc.writeLF("");
            esc.writeLF("");
            esc.writeLF("");
            esc.writeLF("");
            esc.cut(EscPos.CutMode.FULL);
        }
    }

    private static boolean nz(String s){ return s != null && !s.trim().isEmpty(); }
    private static String nv(String s){ return s == null ? "" : s; }
    private static String corteA80(String s){ return s.length() <= 48 ? s : s.substring(0, 48); }
}