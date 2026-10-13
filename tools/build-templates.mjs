// Ejecutar con el Node y @oai/artifact-tool del runtime de artefactos.
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { Workbook, SpreadsheetFile } from '@oai/artifact-tool';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const schema = JSON.parse(await fs.readFile(path.join(root, 'tools/template-schema.json'), 'utf8'));
const output = path.join(root, 'outputs/educontrol-inicial');
const publicDir = path.join(root, 'frontend/public/plantillas');
await fs.mkdir(output, { recursive: true });
await fs.mkdir(publicDir, { recursive: true });
const book = Workbook.create();
const col = i => String.fromCharCode(65 + i);
for (const def of schema) {
  const sheet = book.worksheets.add(def.sheet);
  const end = col(def.headers.length - 1);
  sheet.showGridLines = false;
  sheet.getRange(`A1:${end}201`).format.font = { name: 'Arial', size: 11, color: '#263C32' };
  sheet.getRange(`A1:${end}201`).format.verticalAlignment = 'center';
  sheet.getRange(`A1:${end}201`).format.columnWidth = 25;
  sheet.getRange(`A1:${end}1`).values = [def.headers];
  sheet.getRange(`A1:${end}1`).format = { fill: '#254D3F', font: { name: 'Arial', size: 11, bold: true, color: '#FFFFFF' }, rowHeight: 40, wrapText: true, horizontalAlignment: 'center', verticalAlignment: 'center' };
  sheet.getRange(`A2:${end}201`).format.rowHeight = 23;
  sheet.getRange(`A2:${end}201`).format.fill = '#FAFCF7';
  sheet.freezePanes.freezeRows(1);
  sheet.freezePanes.freezeColumns(1);
  for (const [index, name] of def.headers.entries()) {
    const range = sheet.getRange(`${col(index)}2:${col(index)}201`);
    if (def.text?.includes(name)) range.setNumberFormat('@');
    if (def.dates?.includes(name)) range.setNumberFormat('yyyy-mm-dd');
    if (def.money?.includes(name)) range.setNumberFormat('#,##0.00');
    if (def.decimals?.includes(name)) range.setNumberFormat('0.00');
    if (def.integers?.includes(name)) range.setNumberFormat('0');
    if (def.lists?.[name]) range.dataValidation = { rule: { type: 'list', values: def.lists[name] } };
  }
  const csv = '\uFEFF' + def.headers.join(';') + '\r\n';
  await fs.writeFile(path.join(publicDir, `${def.id}.csv`), csv, 'utf8');
}
book.recalculate();
for (const def of schema) {
  const sheet = book.worksheets.getItem(def.sheet);
  const end = col(def.headers.length - 1);
  if (JSON.stringify(sheet.getRange(`A1:${end}1`).values[0]) !== JSON.stringify(def.headers)) throw new Error(`Encabezados: ${def.id}`);
  if (sheet.getRange(`A2:${end}201`).values.flat().some(v => v !== null && v !== '')) throw new Error(`Datos inesperados: ${def.id}`);
  const preview = await book.render({ sheetName: def.sheet, range: `A1:${end}6`, scale: 1, format: 'png' });
  await fs.writeFile(path.join(output, `${def.id}.png`), new Uint8Array(await preview.arrayBuffer()));
}
const xlsx = await SpreadsheetFile.exportXlsx(book);
await xlsx.save(path.join(output, 'educontrol-plantillas.xlsx'));
await fs.copyFile(path.join(output, 'educontrol-plantillas.xlsx'), path.join(publicDir, 'educontrol-plantillas.xlsx'));
console.log('8 hojas verificadas con 200 filas vacías, 8 CSV y libro XLSX exportados.');
