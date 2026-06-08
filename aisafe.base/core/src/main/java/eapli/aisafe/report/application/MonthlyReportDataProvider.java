package eapli.aisafe.report.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.report.domain.MonthlyReport;

import java.time.YearMonth;

public interface MonthlyReportDataProvider {
    MonthlyReport generateForMonth(YearMonth period, AreaCode areaCode);
}
