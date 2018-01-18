package com.swagger.apibuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import com.apibuilder.storage.StorageService;

public class ExcelUtil {

	private static Sheet createSheet(Workbook wb, Map<String, CellStyle> styles, String[] titles, String sheetName, boolean isApiInfo)
	{
		Sheet sheet = wb.createSheet(sheetName);

		// turn off gridlines
		sheet.setDisplayGridlines(false);
		sheet.setPrintGridlines(false);
		sheet.setFitToPage(true);
		sheet.setHorizontallyCenter(true);
		PrintSetup printSetup = sheet.getPrintSetup();
		printSetup.setLandscape(true);

		// the following three statements are required only for HSSF
		sheet.setAutobreaks(true);
		printSetup.setFitHeight((short) 1);
		printSetup.setFitWidth((short) 1);

		// the header row: centered text in 48pt font
		Row headerRow = sheet.createRow(0);
		headerRow.setHeightInPoints(12.75f);
		for (int i = 0; i < titles.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(titles[i]);
			cell.setCellStyle(styles.get("header"));
		}
		

		// freeze the first row
		sheet.createFreezePane(0, 1);

		
		
		
		// set column widths, the width is measured in units of 1/256th of a
		// character width
		/*sheet.setColumnWidth(0, 256 * 50);
		sheet.setColumnWidth(1, 256 * 50);
		sheet.setColumnWidth(2, 256 * 50);
		sheet.setColumnWidth(3, 256 * 50);*/
		
		//try auto widths of columns.
		HSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
		// sheet.setZoom(3, 4);
		
		if(isApiInfo) {
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(2);
			sheet.autoSizeColumn(3);
			sheet.setColumnWidth(0, 256 * 45);//title
			sheet.setColumnWidth(1, 256 * 20); //version column
			sheet.setColumnWidth(2, 256 * 60); //description column.
			sheet.setColumnWidth(3, 256 * 60); //URI column.
		}
		else if(titles.length ==7){
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(3);
			sheet.autoSizeColumn(4);
			sheet.setColumnWidth(2, 256 * 50);
			sheet.setColumnWidth(5, 256 * 50); //description column
			sheet.setColumnWidth(6, 256 * 80); //xpath column.
		}
		else{
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(2);
			sheet.autoSizeColumn(3);
			sheet.setColumnWidth(1, 256 * 50);
			sheet.setColumnWidth(4, 256 * 50); //description column
			sheet.setColumnWidth(5, 256 * 80); //xpath column.
		}
		
		return sheet;
	}

	/**
	 * 
	 * @param serviceName
	 * @param operationName
	 * @param titles
	 * @param respTitles
	 * @param reqData
	 * @param rspData
	 * @param bldOptipons
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void createExcel(String serviceName, String operationName,
			String[] reqTitles, String[] respTitles, String[] apiInfoTitles,
			String[][] reqData, String[][] rspData, String[][] apiInfoData, ISDOptions bldOptipons,
			final StorageService storageService) throws IOException, ParseException {
		Workbook wb = new XSSFWorkbook();

		Map<String, CellStyle> styles = createStyles(wb, false);
	    Map<String, CellStyle> headerElementStyles = createStyles(wb, true);

		//Sheet sheet = createSheet(wb, styles, titles, "Business Plan");
	    Sheet apiInfoSheet = createSheet(wb, styles, apiInfoTitles, "APIInfo", true);
		Sheet reqSheet = createSheet(wb, styles, reqTitles, "Request", false);
		Sheet rspSheet = createSheet(wb, styles, respTitles, "Response", false);
		
		
		fillSheet(wb, apiInfoSheet, apiInfoData, styles, headerElementStyles, true);
		fillSheet(wb, reqSheet, reqData, styles, headerElementStyles, false);
		fillSheet(wb, rspSheet, rspData, styles, headerElementStyles, false);

		/*try {
			// Let's try to create the output directory first
			Path p = FileSystems.getDefault().getPath(
					bldOptipons.getISDsDirectory());
			Files.createDirectory(p);
		} catch (Exception oops) {
			// the directory may already exist.
		}*/

		// Write the output to a file
		String fName = serviceName + "."+ operationName + ".xls"+(wb instanceof XSSFWorkbook? "x": "");
		File file = storageService.getRootLocation().resolve(fName).toFile();
		//if (wb instanceof XSSFWorkbook)
			//file += "x";
		FileOutputStream out = new FileOutputStream(file);
		wb.write(out);
		out.close();
		//storageService.store(file);
		
	}
	

	/**
	 * put data in a sheet
	 * 
	 * @param sheet
	 * @param data
	 * @param styles
	 */
	private static void fillSheet(Workbook wb, Sheet sheet, String[][] data, Map<String, CellStyle> styles, Map<String, CellStyle> headerStyles, boolean isApiInfo)
	  {
	    Map<String, CellStyle> headerReqStyles = (Map)((HashMap)styles).clone();
	    
	    Font headerFont1 = wb.createFont();
	    headerFont1.setBoldweight((short)700);
	    headerFont1.setColor(IndexedColors.CORNFLOWER_BLUE
	      .getIndex());
	    

	    int rownum = 1;
	    for (int i = 0; i < data.length; rownum++)
	    {
	      Row row = sheet.createRow(rownum);
	      if (data[i] != null) {
	        for (int j = 0; j < data[i].length; j++)
	        {
	          Cell cell = row.createCell(j);
	          String styleName;
	          switch (j)
	          {
	          case 0: 
	            styleName = "cell_normal";
	            cell.setCellValue(data[i][j]);
	            break;
	          case 1: 
	            styleName = "cell_indented";
	            cell.setCellValue(data[i][j]);
	            break;
	          case 2: 
	            styleName = "cell_normal";
	            cell.setCellValue(data[i][j]);
	            break;
	          case 3: 
	          case 4: 
	          case 5:
	          case 6:
	            styleName = "cell_normal";
	            cell.setCellValue(data[i][j]);
	            break;
	          default: 
	            styleName = data[i][j] != null ? "cell_blue" : 
	              "cell_normal";
	          }
	          //if ((rownum >= 4) && (rownum < 40)) {
	            //cell.setCellStyle((CellStyle)headerStyles.get(styleName));
	          //} else {
	            cell.setCellStyle((CellStyle)styles.get(styleName));
	          //}
	        }
	      }
	      i++;
	    }
	  }

	/**
	 * create a library of cell styles
	 */
	private static Map<String, CellStyle> createStyles(Workbook wb, boolean isHeader) {
	    Map<String, CellStyle> styles = new HashMap();
	    DataFormat df = wb.createDataFormat();
	    

	    Font headerFont = wb.createFont();
	    headerFont.setBoldweight((short)700);
	    CellStyle style = createBorderedStyle(wb);
	    style.setAlignment((short)2);
	    style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE
	      .getIndex());
	    style.setFillPattern((short)1);
	    style.setFont(headerFont);
	    styles.put("header", style);
	    
	    Font headerFont1 = wb.createFont();
	    headerFont1.setBoldweight((short)400);
	    headerFont1.setColor(IndexedColors.BLUE_GREY
	      .getIndex());
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE
	      .getIndex());
	    style.setFillPattern((short)1);
	    style.setFont(headerFont1);
	    style.setWrapText(true);
	    styles.put("request_header", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)2);
	    style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE
	      .getIndex());
	    style.setFillPattern((short)1);
	    style.setFont(headerFont1);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("header_date", style);
	    
	    Font font1 = wb.createFont();
	    font1.setBoldweight((short)400);
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setFont(font1);
	    styles.put("cell_b", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)2);
	    style.setFont(font1);
	    styles.put("cell_b_centered", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)3);
	    style.setFont(font1);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("cell_b_date", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)3);
	    style.setFont(font1);
	    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
	    style.setFillPattern((short)1);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("cell_g", style);
	    
	    Font font2 = wb.createFont();
	    font2.setColor(IndexedColors.BLUE.getIndex());
	    font2.setBoldweight((short)400);
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setFont(font2);
	    styles.put("cell_bb", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)3);
	    style.setFont(font1);
	    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
	    style.setFillPattern((short)1);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("cell_bg", style);
	    
	    Font font3 = wb.createFont();
	    font3.setFontHeightInPoints((short)14);
	    font3.setColor(IndexedColors.DARK_BLUE.getIndex());
	    font3.setBoldweight((short)400);
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setFont(font3);
	    style.setWrapText(true);
	    styles.put("cell_h", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setWrapText(true);
	    styles.put("cell_normal", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)2);
	    style.setWrapText(true);
	    styles.put("cell_normal_centered", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)3);
	    style.setWrapText(true);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("cell_normal_date", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setIndention((short)1);
	    style.setWrapText(true);
	    styles.put("cell_indented", style);
	    
	    style = createBorderedStyle(wb);
	    style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
	    style.setFillPattern((short)1);
	    styles.put("cell_blue", style);
	    if (isHeader)
	    {
	      Iterator<CellStyle> cs = styles.values().iterator();
	      while (cs.hasNext())
	      {
	        CellStyle st = (CellStyle)cs.next();
	        st.setFont(headerFont1);
	      }
	    }
	    return styles;
	}

	private static CellStyle createBorderedStyle(Workbook wb) {
		CellStyle style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderBottom(CellStyle.BORDER_THIN);
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderTop(CellStyle.BORDER_THIN);
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		return style;
	}
}
