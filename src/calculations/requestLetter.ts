import type { Company, Language } from '../types.js';

export interface RequestLetterInput {
  company: Company;
  language: Language;
  includeMetrics: { profile: boolean; volatility: boolean; shape: boolean };
}

const LOCALE_TAGS: Record<Language, string> = {
  en: 'en-US',
  et: 'et-EE',
  lv: 'lv-LV',
  lt: 'lt-LT',
};

const METRIC_LABELS: Record<Language, { profile: string; volatility: string; shape: string }> = {
  en: {
    profile: 'consumption-pattern deviation vs. a typical Baltic SME load profile',
    volatility: 'month-to-month consumption volatility',
    shape: 'shape-vs-flat-load cost impact against the spot market',
  },
  et: {
    profile: 'tarbimismustri hälve tüüpilisest Balti VKE koormusprofiilist',
    volatility: 'kuust kuusse tarbimise kõikuvus',
    shape: 'tarbimisgraafiku mõju maksumusele börsihinna suhtes',
  },
  lv: {
    profile: 'patēriņa profila novirze no tipiska Baltijas MVU slodzes profila',
    volatility: 'patēriņa svārstīgums no mēneša uz mēnesi',
    shape: 'patēriņa grafika ietekme uz izmaksām salīdzinājumā ar biržas cenu',
  },
  lt: {
    profile: 'suvartojimo profilio nuokrypis nuo tipinio Baltijos MVĮ apkrovos profilio',
    volatility: 'suvartojimo svyravimas iš mėnesio į mėnesį',
    shape: 'suvartojimo grafiko įtaka kainai lyginant su biržos kaina',
  },
};

const COPY: Record<
  Language,
  {
    subject: string;
    greeting: string;
    intro: (company: Company, annualKwh: string) => string;
    metricsIntro: string;
    tableTitle: string;
    tableHeaders: [string, string];
    rowSpot: string;
    rowFixed: (months: number) => string;
    closing: string;
    signOff: string;
  }
> = {
  en: {
    subject: 'Request for Electricity Supply Quote',
    greeting: 'Dear Supplier,',
    intro: (company, annualKwh) =>
      `We are requesting a quote for electricity supply on behalf of ${company.name} ` +
      `(registry code ${company.registryCode}, EIC code ${company.eicCode}), with an ` +
      `estimated annual consumption of ${annualKwh} kWh.`,
    metricsIntro: 'For reference, please take the following consumption characteristics into account:',
    tableTitle: 'Please provide rate offers for the following contract options:',
    tableHeaders: ['Plan', 'Term'],
    rowSpot: 'Spot + margin | Ongoing (spot-indexed, monthly margin)',
    rowFixed: (months) => `Fixed rate | ${months} months`,
    closing: 'We look forward to receiving your offer within 5 business days.',
    signOff: 'Kind regards,',
  },
  et: {
    subject: 'Elektrienergia tarnepakkumise päring',
    greeting: 'Lugupeetud tarnija,',
    intro: (company, annualKwh) =>
      `Palume esitada elektrienergia tarnepakkumine ettevõttele ${company.name} ` +
      `(registrikood ${company.registryCode}, EIC kood ${company.eicCode}), hinnanguline ` +
      `aastane tarbimine on ${annualKwh} kWh.`,
    metricsIntro: 'Palun arvestage pakkumise koostamisel järgmiste tarbimisnäitajatega:',
    tableTitle: 'Palume esitada hinnapakkumised järgmiste lepingutingimuste kohta:',
    tableHeaders: ['Pakett', 'Periood'],
    rowSpot: 'Börsihind + marginaal | Tähtajatu (börsipõhine, igakuine marginaal)',
    rowFixed: (months) => `Fikseeritud hind | ${months} kuud`,
    closing: 'Ootame teie pakkumist 5 tööpäeva jooksul.',
    signOff: 'Lugupidamisega,',
  },
  lv: {
    subject: 'Pieprasījums elektroenerģijas piegādes piedāvājumam',
    greeting: 'Godātais piegādātāj,',
    intro: (company, annualKwh) =>
      `Lūdzam iesniegt elektroenerģijas piegādes piedāvājumu uzņēmumam ${company.name} ` +
      `(reģistrācijas kods ${company.registryCode}, EIC kods ${company.eicCode}), ` +
      `aptuvenais gada patēriņš ir ${annualKwh} kWh.`,
    metricsIntro: 'Lūdzam piedāvājuma sagatavošanā ņemt vērā šādus patēriņa rādītājus:',
    tableTitle: 'Lūdzam iesniegt cenu piedāvājumus šādiem līguma nosacījumiem:',
    tableHeaders: ['Plāns', 'Termiņš'],
    rowSpot: 'Biržas cena + rezerve | Nenoteikts (biržas cena, ikmēneša rezerve)',
    rowFixed: (months) => `Fiksēta likme | ${months} mēneši`,
    closing: 'Gaidīsim jūsu piedāvājumu 5 darba dienu laikā.',
    signOff: 'Ar cieņu,',
  },
  lt: {
    subject: 'Prašymas pateikti elektros energijos tiekimo pasiūlymą',
    greeting: 'Gerbiamas tiekėjau,',
    intro: (company, annualKwh) =>
      `Prašome pateikti elektros energijos tiekimo pasiūlymą įmonei ${company.name} ` +
      `(registracijos kodas ${company.registryCode}, EIC kodas ${company.eicCode}), ` +
      `numatomas metinis suvartojimas yra ${annualKwh} kWh.`,
    metricsIntro: 'Rengiant pasiūlymą prašome atsižvelgti į šiuos suvartojimo rodiklius:',
    tableTitle: 'Prašome pateikti kainų pasiūlymus šioms sutarties sąlygoms:',
    tableHeaders: ['Planas', 'Terminas'],
    rowSpot: 'Biržos kaina + marža | Neterminuota (biržos kaina, mėnesinė marža)',
    rowFixed: (months) => `Fiksuotas tarifas | ${months} mėn.`,
    closing: 'Lauksime jūsų pasiūlymo per 5 darbo dienas.',
    signOff: 'Pagarbiai,',
  },
};

const FIXED_TERM_MONTHS = [6, 12, 24];

/**
 * ARCH_SPEC.md §2 "RFQ request-letter text": deterministic template string interpolating
 * company name/registry code/EIC/annual kWh/included-metric labels into a fixed multi-paragraph
 * format with a rate-offer table (spot+margin, fixed 6/12/24mo rows), localized per language.
 */
export function generateRequestLetterText(input: RequestLetterInput): string {
  const { company, language, includeMetrics } = input;
  const copy = COPY[language];
  const metricLabels = METRIC_LABELS[language];
  const annualKwh = company.annualKwh.toLocaleString(LOCALE_TAGS[language]);

  const paragraphs: string[] = [copy.subject, '', copy.greeting, '', copy.intro(company, annualKwh)];

  const selectedMetrics = [
    includeMetrics.profile ? metricLabels.profile : null,
    includeMetrics.volatility ? metricLabels.volatility : null,
    includeMetrics.shape ? metricLabels.shape : null,
  ].filter((label): label is string => label !== null);

  if (selectedMetrics.length > 0) {
    paragraphs.push('', copy.metricsIntro);
    for (const label of selectedMetrics) {
      paragraphs.push(`- ${label}`);
    }
  }

  paragraphs.push(
    '',
    copy.tableTitle,
    `${copy.tableHeaders[0]} | ${copy.tableHeaders[1]}`,
    copy.rowSpot,
    ...FIXED_TERM_MONTHS.map((months) => copy.rowFixed(months)),
    '',
    copy.closing,
    '',
    copy.signOff,
  );

  return paragraphs.join('\n');
}
