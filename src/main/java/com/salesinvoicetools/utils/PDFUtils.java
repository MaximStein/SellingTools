package com.salesinvoicetools.utils;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import com.google.api.client.util.Strings;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import com.salesinvoicetools.dataaccess.AppSettings;
import com.salesinvoicetools.models.*;

public class PDFUtils {
	
	private static int yTop = 720; //der text unter dem logo
	private static int xMargin = 70;
	
	private static int dataTableLeft = xMargin+250;
	private static int dataTableTop = yTop;
	private static int dataTableWidth = 220;
	private static int dataTableHeight = 70;
	
	private static int headlineY = 570;
	private static int tableY = 540;
	private static int tableWidth = 470;
	
	private static int bottomSectionY = 140;
	
	private static Font font = new Font(Font.HELVETICA, 10);		
	private static Font tableHeaderFont = new Font(Font.HELVETICA, 8, Font.BOLD);
	private static Font tableFont = new Font(Font.HELVETICA, 8);
	private static Font footerFont = new Font(Font.HELVETICA, 8);

    public static boolean createInvoicesFile(Map<Long, ShopOrder> orders) {

            var returnVal = true;
            final Document document = new Document(PageSize.A4);
            var min = orders.keySet().stream().min(Long::compareTo).get();
            var max = orders.keySet().stream().max(Long::compareTo).get();
            try {
                var fileName = ""+min+"-"+max+".pdf";
                var directory = AppSettings.getString(AppSettings.INVOICE_OUTPUT_DIRECTORY);
                directory = Strings.isNullOrEmpty(directory) ? "." : directory;

                var writer = PdfWriter.getInstance(document, new FileOutputStream(directory+"/"+fileName));
                document.open();


                orders.forEach((invoiceNumber, shopOrder) -> {
                   PDFUtils.writeOrderInvoice(writer, shopOrder, invoiceNumber);
                    document.add(new Chunk("")); // << this will do the trick.
                    writer.setPageEmpty(false);
                    document.newPage();
                });


                document.close();
            } catch (DocumentException de) {
                System.err.println(de.getMessage());
                returnVal = false;

            } catch (IOException ioe) {

                returnVal = false;
                System.err.println(ioe.getMessage());
            }

            document.close();

            return returnVal;

    }

    private static void writeOrderInvoice(PdfWriter writer, ShopOrder order, long invoiceNumber) {
        var addr = AppSettings.get(AppSettings.INVOICE_ADDRESS, Address.class);

        if(addr != null) {
            var name = addr.additionalInfo == null || addr.additionalInfo.isBlank() ? addr.name : addr.additionalInfo;
            Chunk senderText = new Chunk(name+", "+addr.getStreet()+", "+addr.getPostalCode()+" "+addr.getCity(), new Font(Font.HELVETICA, 10, Font.UNDERLINE));
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, new Paragraph(senderText), 1f*xMargin, 1f*yTop,0f);
        }

        var orderAddr = order.getBillingsAddress() == null ? order.getShippingAddress() : order.getBillingsAddress();
        Chunk billingAddressText = new Chunk(orderAddr.toString(), font);
        ColumnText ct = new ColumnText(writer.getDirectContent());
        ct.setSimpleColumn(xMargin, 0, xMargin+1000, yTop);
        ct.addText(new Paragraph(billingAddressText));
        //ct.setYLine(50);
        ct.go();

        PdfContentByte cb = writer.getDirectContentUnder();
        cb.saveState();
        cb.setColorFill(Color.LIGHT_GRAY);
        cb.rectangle(dataTableLeft, dataTableTop - dataTableHeight, dataTableWidth, dataTableHeight);
        cb.fill();
        cb.rectangle(xMargin, bottomSectionY, PageSize.A4.getWidth() - 2*xMargin, 1);
        cb.fill();
        cb.restoreState();

        var billingDateStr = AppUtils.formatDate(order.getInvoice() == null ? Timestamp.from(Instant.now()): order.getInvoice().getInvoiceTime());
        var deliveryDateStr = order.getShippedTime() == null ? "" : AppUtils.formatDate(order.getShippedTime());
        var orderDateStr = order.getOrderTime() == null ? "" : AppUtils.formatDate(order.getOrderTime());

        var table = new PdfPTable(new float[]{.5f, .5f});
        table.setTotalWidth(dataTableWidth);

        table.addCell(getTableCell("Liefer-/Leistungsdatum"));
        table.addCell(getTableCell(deliveryDateStr));

        table.addCell(getTableCell("Rechnungsdatum"));
        table.addCell(getTableCell(billingDateStr));

        table.addCell(getTableCell("Bestelldatum"));
        table.addCell(getTableCell(orderDateStr));

        table.addCell(getTableCell("Referenznummer"));
        table.addCell(getTableCell(order.getOrderNumber()));

        table.writeSelectedRows(0, -1, dataTableLeft, yTop, writer.getDirectContent());


        var headline = new Chunk("Rechnung Nr. "+invoiceNumber, new Font(Font.HELVETICA, 16, Font.BOLD));
        ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, new Phrase(headline), xMargin, headlineY, 0);

        table = new PdfPTable(new float[]{.07f, .18f, .6f, .1f, .125f, .125f});
        table.setTotalWidth(tableWidth);

        table.addCell(getTableHeaderCell("Pos."));
        table.addCell(getTableHeaderCell("Artikelnummer"));
        table.addCell(getTableHeaderCell("Bezeichnung"));
        table.addCell(getTableHeaderCell("Anzahl", Element.ALIGN_RIGHT));
        table.addCell(getTableHeaderCell("Preis", Element.ALIGN_RIGHT));
        table.addCell(getTableHeaderCell("Summe", Element.ALIGN_RIGHT));

        int posNr = 1;
        for (LineItem item : order.getItems()) {
            table.addCell(getTableCell(""+posNr,1, Element.ALIGN_LEFT, Table.BOTTOM));
            table.addCell(getTableCell(item.getProduct().getProductNumber(),1, Element.ALIGN_LEFT, Table.BOTTOM));
            table.addCell(getTableCell(item.getProduct().getTitle(),1, Element.ALIGN_LEFT, Table.BOTTOM));
            table.addCell(getTableCell(""+item.getQuantity(),1, Element.ALIGN_RIGHT, Table.BOTTOM));
            table.addCell(getTableCell(AppUtils.formatCurrencyAmount(item.getTotalPriceGross() / item.getQuantity()),1, Element.ALIGN_RIGHT, Table.BOTTOM));
            table.addCell(getTableCell(AppUtils.formatCurrencyAmount(item.getTotalPriceGross()),1, Element.ALIGN_RIGHT, Table.BOTTOM));
            posNr++;
        }

        var emptyCell = getTableCell("", table.getNumberOfColumns(), 0, 0);
        emptyCell.setFixedHeight(10);
        table.addCell(emptyCell);

        table.addCell(getTableCell("Zwischensumme", 3, Element.ALIGN_RIGHT));
        table.addCell(getTableCell(AppUtils.formatCurrencyAmount(order.getItemsTotalAmount()), 3, Element.ALIGN_RIGHT));

        table.addCell(getTableCell("Versandkosten", 3, Element.ALIGN_RIGHT));
        table.addCell(getTableCell(AppUtils.formatCurrencyAmount(order.getShippingCosts()), 3, Element.ALIGN_RIGHT));
        table.addCell(getTableCell("gesamt Netto", 3, Element.ALIGN_RIGHT));
        table.addCell(getTableCell(AppUtils.formatCurrencyAmount(order.getGrandTotalAmount() - order.getVatAmount()), 3, Element.ALIGN_RIGHT));
        table.addCell(getTableCell("Umsatzsteuer ("+order.getVatPercent()+"%)", 3, Element.ALIGN_RIGHT));
        table.addCell(getTableCell(AppUtils.formatCurrencyAmount(order.getVatAmount()), 3, Element.ALIGN_RIGHT));

        table.addCell(emptyCell);

        var boldFont = new Font(tableFont.getFamily(), tableFont.getSize(), Font.BOLD);

        var grandTotalLabelCell = new PdfPCell(new Phrase("Gesamtsumme", boldFont));
        grandTotalLabelCell.setColspan(3);
        grandTotalLabelCell.setBorder(0);
        grandTotalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        var grandTotalCell = new PdfPCell(new Phrase(AppUtils.formatCurrencyAmount(order.getGrandTotalAmount()), boldFont));
        grandTotalCell.setColspan(3);
        grandTotalCell.setBorder(0);
        grandTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(grandTotalLabelCell);
        table.addCell(grandTotalCell);

        var tableBottom = table.writeSelectedRows(0, -1, xMargin, tableY, writer.getDirectContent());

        var kleinUnterNehmerInfo = AppSettings.getString(AppSettings.KLEINUNTERNEHMER_INFO);
        if(!Strings.isNullOrEmpty(kleinUnterNehmerInfo)) {
            ct = new ColumnText(writer.getDirectContent());
            ct.setAlignment(Element.ALIGN_CENTER);
            ct.setSimpleColumn(0, 0, PageSize.A4.getWidth(), tableBottom);
            ct.addText(new Paragraph("\r\n"+kleinUnterNehmerInfo, font));
            ct.go();
        }

        ct = new ColumnText(writer.getDirectContent());
        ct.setSimpleColumn(xMargin, 0, 1000, bottomSectionY);

        if(addr != null)
            ct.addText(new Paragraph(addr.toString(), footerFont));
        ct.go();

        ct = new ColumnText(writer.getDirectContent());
        var middleBottomStr = "";
        var bankInfo = AppSettings.get(AppSettings.BANK_INFO, BankInfo.class);
        if(AppSettings.getBoolean(AppSettings.BANK_INFO_ON_INVOICE) && bankInfo != null)
            middleBottomStr += bankInfo.toString();
        middleBottomStr += "\r\nUst.-Id.: "+AppSettings.getString(AppSettings.VAT_ID);
        ct.setSimpleColumn(xMargin + 155, 0, 1000, bottomSectionY);
        ct.addText(new Paragraph(middleBottomStr, footerFont));
        ct.go();

        ct = new ColumnText(writer.getDirectContent());
        ct.setSimpleColumn(xMargin + 330, 0, 1000, bottomSectionY);
        var contactInfo =AppSettings.get(AppSettings.CONTACT_INFO, ContactInfo.class);
        if(contactInfo != null)
            ct.addText(new Paragraph(contactInfo.toString(), footerFont));
        ct.go();
        //writer.setPageEmpty(false);

    }

	/**
	 * generated an invoice in the output directory that was specified inside the AppConfiguration object provided
	 * does >not< update the invoice number of the order or the settings object 
	 * @param order the ShopOrder for the invoice
	 * @param settings the AppConfiguration object 
	 * @param invoiceNumber the invoice number on the order
	 * @return true if successful, otherwise false
	 */
	public static boolean createInvoiceFile(ShopOrder order, AppConfiguration settings, long invoiceNumber) {
		
		var returnVal = true;
		Document document = new Document(PageSize.A4);
        try {          
        	var directory = Strings.isNullOrEmpty(settings.getInvoiceDirectory()) ? "./"+invoiceNumber+".pdf" : settings.getInvoiceDirectory()+"/"+invoiceNumber+".pdf"; 
            var writer = PdfWriter.getInstance(document, new FileOutputStream(directory));
            document.open();

            PDFUtils.writeOrderInvoice(writer,order,invoiceNumber);

        } catch (DocumentException de) {
            System.err.println(de.getMessage());
            returnVal = false;
            
        } catch (IOException ioe) {
        	
        	returnVal = false;
            System.err.println(ioe.getMessage());
        }

        document.close();
        
        return returnVal;
	}
		
	private static PdfPCell getTableHeaderCell(String text, int hAlign) {
		var cell = getTableHeaderCell(text);
		cell.setHorizontalAlignment(hAlign);
	
		return cell;
	}
	
	private static PdfPCell getTableHeaderCell(String text) {
		 var cell = new PdfPCell(new Phrase(text, tableHeaderFont));		 
         cell.setBackgroundColor(Color.LIGHT_GRAY);     	
     	cell.setBorder(Table.BOTTOM);
         return cell;
	}
	
	private static PdfPCell getTableCell(String text, int colSpan, int hAlign, int border) {
		var cell = getTableCell(text, colSpan, hAlign);
		cell.setBorder(border);
		return cell;
	}
	
	private static PdfPCell getTableCell(String text, int colSpan, int hAlign) {
		var cell = getTableCell(text, colSpan);
		cell.setHorizontalAlignment(hAlign);
		return cell;
	}
	
	private static PdfPCell getTableCell(String text, int colSpan) {
		var cell = getTableCell(text);
		cell.setColspan(colSpan);	
		return cell;
	}
	
	private static PdfPCell getTableCell(String text) {
		var cell = new PdfPCell(new Phrase(text, tableFont));		
		cell.setBorder(0);
		return cell;
		
	}
}
