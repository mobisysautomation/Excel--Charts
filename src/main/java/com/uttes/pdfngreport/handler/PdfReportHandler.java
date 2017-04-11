package com.uttes.pdfngreport.handler;
/*
 Copyright 2015 Uttesh Kumar T.H.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import com.uttesh.pdfngreport.common.Constants;
import com.uttesh.pdfngreport.util.PdfLogger;
import com.uttesh.pdfngreport.util.PdfngUtil;
import com.uttesh.pdfngreport.util.chart.ChartStyle;
import com.uttesh.pdfngreport.util.xml.ReportData;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;

/**
 * This is a handler class for the pdf report generation, it generate the
 * jfreechart image and xsl fo pdf report
 *
 * @author Uttesh Kumar T.H.
 * @version 2.0.0
 * @since
 */
public class PdfReportHandler {

    PdfLogger logger = PdfLogger.getLogger(PdfReportHandler.class.getName());

    String reportLocation = PdfngUtil.getReportLocation();

    /**
     * This method generate the XML data file required by the apache fo to
     * generate the PDF report. XML data file is generated by using the java
     * JAXB.
     *
     * @param reportData
     * @return UUID which is used as the file name.
     *
     * {@link ReportData#member ReportData}
     * @see JAXBException
     */
    public int generateXmlData(ReportData reportData) {
        JAXBContext jc;
        int fileName = UUID.randomUUID().toString().substring(0, 3).hashCode();
        File file = null;
        try {
            jc = JAXBContext.newInstance(ReportData.class);
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            if (reportData.getOsName().equalsIgnoreCase("w")) {
                file = new File(reportLocation + Constants.BACKWARD_SLASH + fileName + Constants.XML_EXTENSION);
            } else {
                file = new File(reportLocation + Constants.FORWARD_SLASH + fileName + Constants.XML_EXTENSION);
            }
            marshaller.marshal(reportData, file);
        } catch (JAXBException ex) {
            Logger.getLogger(PdfReportHandler.class.getName()).log(Level.SEVERE, null, ex);
            //new File(reportLocation + Constants.FORWARD_SLASH + fileName + Constants.XML_EXTENSION).delete();
        }
        return fileName;
    }

    /**
     * This method generate the PDF report by report data and the report store
     * location. After the report generation it will remove the temp file
     * created by the generator i.e XML data file and chart image file.
     *
     * @param reportData
     * @param reportFile
     * @throws FileNotFoundException
     * @throws FOPException
     * @throws TransformerConfigurationException
     * @throws TransformerException
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     *
     * @see FileNotFoundException
     * @see FOPException
     * @see TransformerConfigurationException
     * @see TransformerException
     * @see IOException
     * @see InterruptedException
     * @see URISyntaxException
     *
     * {@link ReportData#member ReportData}
     */
    public void generatePdfReport(ReportData reportData, File reportFile) throws FileNotFoundException, FOPException, TransformerConfigurationException, TransformerException, IOException, InterruptedException, URISyntaxException {
        int fileName = generateXmlData(reportData);
        File xmlfile = null;
        if (reportData.getOsName().equalsIgnoreCase("w")) {
            xmlfile = new File(reportLocation + Constants.BACKWARD_SLASH + fileName + Constants.XML_EXTENSION);
        } else {
            xmlfile = new File(reportLocation + Constants.FORWARD_SLASH + fileName + Constants.XML_EXTENSION);
        }
        //File xsltfile = new File(getClass().getClassLoader().getResource(Constants.REPORT_XSL_TEMPLATE).toURI());
        InputStream input = getClass().getClassLoader().getResourceAsStream(Constants.REPORT_XSL_TEMPLATE);
        File pdffile = reportFile;
        FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        OutputStream out = new java.io.FileOutputStream(pdffile);
        out = new java.io.BufferedOutputStream(out);
        try {
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(input));
            transformer.setParameter("versionParam", "2.0");
            Source src = new StreamSource(xmlfile);
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, res);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (reportData.getOsName().equalsIgnoreCase("w")) {
                new File(reportLocation + Constants.BACKWARD_SLASH + fileName + Constants.XML_EXTENSION).delete();
            } else {
                new File(reportLocation + Constants.FORWARD_SLASH + fileName + Constants.XML_EXTENSION).delete();
            }

            System.exit(-1);
        } finally {
            out.close();
            if (reportData.getOsName().equalsIgnoreCase("w")) {
                new File(reportLocation + Constants.BACKWARD_SLASH + fileName + Constants.XML_EXTENSION).delete();
                new File(reportLocation + Constants.BACKWARD_SLASH + Constants.REPORT_CHART_FILE).delete();
            } else {
                new File(reportLocation + Constants.FORWARD_SLASH + fileName + Constants.XML_EXTENSION).delete();
                new File(reportLocation + Constants.FORWARD_SLASH + Constants.REPORT_CHART_FILE).delete();
            }

        }
    }

    /**
     * This method will generate the chart image file by using the Jfree chart
     * library
     *
     * @param dataSet
     * @throws FileNotFoundException
     * @throws IOException
     *
     * @see DefaultPieDataset
     */
    public void generateChart(DefaultPieDataset dataSet,String os) throws FileNotFoundException, IOException {
        try {
            JFreeChart chart = ChartFactory.createPieChart3D("", dataSet, true, true, false);
            ChartStyle.theme(chart);
            PiePlot3D plot = (PiePlot3D) chart.getPlot();
            plot.setForegroundAlpha(0.6f);
            plot.setCircular(true);
            plot.setSectionPaint("Passed", Color.decode("#39e600"));
            plot.setSectionPaint("Failed", Color.decode("#ff3300"));
            plot.setSectionPaint("Skipped", Color.decode("#ffcc00"));
            Color transparent = new Color(0.0f, 0.0f, 0.0f, 0.0f);
            plot.setLabelOutlinePaint(transparent);
            plot.setLabelBackgroundPaint(transparent);
            plot.setLabelShadowPaint(transparent);
            plot.setLabelLinkPaint(Color.GRAY);
            Font font = new Font("SansSerif", Font.PLAIN, 10);
            plot.setLabelFont(font);
            PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator("{0}: {1} ({2})", new DecimalFormat("0"), new DecimalFormat("0%"));
            plot.setLabelGenerator(gen);
            if (os != null && os.equalsIgnoreCase("w")) {
                ChartUtilities.saveChartAsPNG(new File(reportLocation + Constants.BACKWARD_SLASH + Constants.REPORT_CHART_FILE), chart, 560, 200);
            } else {
                ChartUtilities.saveChartAsPNG(new File(reportLocation + Constants.FORWARD_SLASH + Constants.REPORT_CHART_FILE), chart, 560, 200);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (os != null && os.equalsIgnoreCase("w")) {
                new File(reportLocation + Constants.BACKWARD_SLASH + Constants.REPORT_CHART_FILE).delete();
            } else {
                new File(reportLocation + Constants.FORWARD_SLASH + Constants.REPORT_CHART_FILE).delete();
            }
            System.exit(-1);
        }
    }

    /**
     * Not Used for the current release
     *
     * @param dataSet
     */
    public void pieExplodeChart(DefaultPieDataset dataSet,String os) {
        try {
            JFreeChart chart = ChartFactory.createPieChart("", dataSet, true, true, false);
            ChartStyle.theme(chart);
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setForegroundAlpha(0.6f);
            plot.setCircular(true);
            plot.setSectionPaint("Passed", Color.decode("#019244"));
            plot.setSectionPaint("Failed", Color.decode("#EE6044"));
            plot.setSectionPaint("Skipped", Color.decode("#F0AD4E"));
            Color transparent = new Color(0.0f, 0.0f, 0.0f, 0.0f);
            //plot.setLabelLinksVisible(Boolean.FALSE);
            plot.setLabelOutlinePaint(transparent);
            plot.setLabelBackgroundPaint(transparent);
            plot.setLabelShadowPaint(transparent);
            plot.setLabelLinkPaint(Color.GRAY);
            Font font = new Font("SansSerif", Font.PLAIN, 10);
            plot.setLabelFont(font);
            plot.setLabelPaint(Color.DARK_GRAY);
            plot.setExplodePercent("Passed", 0.10);
            //plot.setExplodePercent("Failed", 0.10);
            //plot.setExplodePercent("Skipped", 0.10);
            plot.setSimpleLabels(true);
            PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator("{1} ({2})", new DecimalFormat("0"), new DecimalFormat("0%"));
            plot.setLabelGenerator(gen);

            if (os != null && os.equalsIgnoreCase("w")) {
                ChartUtilities.saveChartAsPNG(new File(reportLocation + Constants.BACKWARD_SLASH + Constants.REPORT_CHART_FILE), chart, 560, 200);
            } else {
                ChartUtilities.saveChartAsPNG(new File(reportLocation + Constants.FORWARD_SLASH + Constants.REPORT_CHART_FILE), chart, 560, 200);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (os != null && os.equalsIgnoreCase("w")) {
                new File(reportLocation + Constants.BACKWARD_SLASH + Constants.REPORT_CHART_FILE).delete();
            } else {
                new File(reportLocation + Constants.FORWARD_SLASH + Constants.REPORT_CHART_FILE).delete();
            }
            System.exit(-1);
        }
    }

}
