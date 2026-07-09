package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.model.Chamado;
import com.bat.uberlandia.dashboard.repository.ChamadoRepository;
import com.bat.uberlandia.dashboard.service.ChamadoService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final ChamadoRepository chamadoRepository;
    private final ChamadoService chamadoService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @GetMapping
    public String paginaRelatorios(Model model) {
        long total = chamadoRepository.count();
        long concluidos = chamadoRepository.findByStatus(Chamado.Status.CONCLUIDO).size();
        long abertos = chamadoRepository.findByStatus(Chamado.Status.ABERTO).size();
        double mttr = chamadoService.getTempoMedioReparoGeral();

        model.addAttribute("totalChamados", total);
        model.addAttribute("totalConcluidos", concluidos);
        model.addAttribute("totalAbertos", abertos);
        model.addAttribute("mttr", mttr);
        return "relatorios";
    }

    @GetMapping("/excel")
    public void exportarExcel(HttpServletResponse response,
                              @RequestParam(required = false) Chamado.Status status) throws IOException {

        List<Chamado> chamados = (status != null)
                ? chamadoRepository.findByStatus(status)
                : chamadoRepository.findAll();

        String nomeArquivo = "relatorio_chamados_bat";
        if (status != null) nomeArquivo += "_" + status.name();
        nomeArquivo += ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"");

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Chamados");

        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle titleStyle = wb.createCellStyle();
        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BAT Brasil - Relatorio de Chamados de Manutencao");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

        Row subtitleRow = sheet.createRow(1);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("Gerado em: " + LocalDateTime.now().format(FMT)
                + (status != null ? " | Filtro: " + status.name() : " | Todos os chamados"));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

        String[] headers = {"ID", "Titulo", "Maquina", "Setor", "Motivo Falha",
                "Status", "Tecnico", "Especialista", "Abertura", "Conclusao", "Tempo Reparo (min)"};

        Row headerRow = sheet.createRow(3);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 4;
        for (Chamado c : chamados) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getTitulo() != null ? c.getTitulo() : "");
            row.createCell(2).setCellValue(c.getMaquina() != null ? c.getMaquina().getNome() : "-");
            row.createCell(3).setCellValue(c.getMaquina() != null && c.getMaquina().getSetor() != null
                    ? c.getMaquina().getSetor().getNome() : "-");
            row.createCell(4).setCellValue(c.getMotivoFalha() != null ? c.getMotivoFalha().name() : "-");
            row.createCell(5).setCellValue(c.getStatus().name());
            row.createCell(6).setCellValue(c.getTecnico() != null ? c.getTecnico().getNome() : "-");
            row.createCell(7).setCellValue(c.getEspecialista() != null ? c.getEspecialista().getNome() : "-");
            row.createCell(8).setCellValue(c.getDataAbertura() != null ? c.getDataAbertura().format(FMT) : "-");
            row.createCell(9).setCellValue(c.getDataConclusao() != null ? c.getDataConclusao().format(FMT) : "-");
            long tempoMin = (c.getTempoReparoAcumulado() != null) ? c.getTempoReparoAcumulado() / 60 : 0;
            row.createCell(10).setCellValue(tempoMin);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        Sheet metricsSheet = wb.createSheet("Metricas");
        Row mTitle = metricsSheet.createRow(0);
        mTitle.createCell(0).setCellValue("Metricas Gerais");
        mTitle.getCell(0).setCellStyle(titleStyle);
        metricsSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        Map<Chamado.MotivoFalha, Long> dist = chamadoService.getDistribuicaoMotivosFalha(null);
        int mr = 2;
        metricsSheet.createRow(mr++).createCell(0).setCellValue("Total de chamados:");
        metricsSheet.getRow(mr - 1).createCell(1).setCellValue(chamados.size());
        metricsSheet.createRow(mr++).createCell(0).setCellValue("MTTR geral (min):");
        metricsSheet.getRow(mr - 1).createCell(1).setCellValue(chamadoService.getTempoMedioReparoGeral());
        mr++;

        metricsSheet.createRow(mr++).createCell(0).setCellValue("Distribuicao por Motivo de Falha");
        for (Map.Entry<Chamado.MotivoFalha, Long> e : dist.entrySet()) {
            metricsSheet.createRow(mr).createCell(0).setCellValue(e.getKey().name());
            metricsSheet.getRow(mr).createCell(1).setCellValue(e.getValue());
            mr++;
        }
        metricsSheet.autoSizeColumn(0);
        metricsSheet.autoSizeColumn(1);

        wb.write(response.getOutputStream());
        wb.close();
    }
}