import json
from pathlib import Path
from xml.sax.saxutils import escape
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, KeepTogether
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

root=Path(__file__).resolve().parents[1]
data=json.loads((root/'frontend/public/manuales/convivencia.json').read_text(encoding='utf-8'))
out=root/'output/pdf/manual-convivencia-centro-saber.pdf'
out.parent.mkdir(parents=True,exist_ok=True)
pdfmetrics.registerFont(TTFont('Manual','C:/Windows/Fonts/arial.ttf'))
pdfmetrics.registerFont(TTFont('ManualBold','C:/Windows/Fonts/arialbd.ttf'))
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='ManualBody',fontName='Manual',fontSize=10.5,leading=16,spaceAfter=10,textColor=colors.HexColor('#243f50')))
styles.add(ParagraphStyle(name='ManualTitle',fontName='ManualBold',fontSize=26,leading=32,spaceAfter=20,textColor=colors.HexColor('#0876b3')))
styles.add(ParagraphStyle(name='ManualHeading',fontName='ManualBold',fontSize=15,leading=20,spaceBefore=17,spaceAfter=10,textColor=colors.HexColor('#075e8f'),keepWithNext=True))
styles.add(ParagraphStyle(name='ManualSmall',fontName='Manual',fontSize=9,leading=13,spaceAfter=10,textColor=colors.HexColor('#597486')))
styles.add(ParagraphStyle(name='ManualItem',parent=styles['ManualBody'],leftIndent=15,firstLineIndent=-10,spaceAfter=8))

def page(canvas,doc):
    width,height=A4
    canvas.saveState()
    canvas.setFillColor(colors.HexColor('#0876b3'));canvas.rect(0,height-16,width,16,fill=1,stroke=0)
    canvas.setFont('ManualBold',9);canvas.drawString(48,height-39,'CENTRO SABER | CONVIVENCIA')
    canvas.setStrokeColor(colors.HexColor('#c9dfe9'));canvas.line(48,42,width-48,42)
    canvas.setFillColor(colors.HexColor('#597486'));canvas.setFont('Manual',8)
    canvas.drawString(48,29,'Propuesta para revisión institucional - Versión 0.1 - 13/10/2026')
    canvas.drawRightString(width-48,29,f'Página {doc.page}')
    canvas.restoreState()

story=[Spacer(1,15),Paragraph(escape(data['title']),styles['ManualTitle']),Paragraph(escape(data['status'])+' | Versión '+data['version'],styles['ManualHeading']),Paragraph(escape(data['intro']),styles['ManualBody']),Paragraph('Fecha de la propuesta: '+escape(data['date']),styles['ManualSmall'])]
for section in data['sections']:
    story.append(Paragraph(escape(section['title']),styles['ManualHeading']))
    for text in section['paragraphs']:story.append(KeepTogether([Paragraph(escape(text),styles['ManualBody'])]))
    for text in section.get('items',[]):story.append(KeepTogether([Paragraph('- '+escape(text),styles['ManualItem'])]))
story.append(Paragraph('Referencias para la revisión',styles['ManualHeading']))
for source in data['sources']:
    story.append(Paragraph(f'<a href="{escape(source["url"])}" color="#0876b3">{escape(source["title"])}</a>',styles['ManualBody']))
    story.append(Paragraph(escape(source['url']),styles['ManualSmall']))
doc=SimpleDocTemplate(str(out),pagesize=A4,rightMargin=48,leftMargin=48,topMargin=60,bottomMargin=60,title='Centro Saber - Propuesta de manual de convivencia',author='Centro Saber',pageCompression=1)
doc.build(story,onFirstPage=page,onLaterPages=page)
public=root/'frontend/public/manuales/manual-convivencia-centro-saber.pdf'
public.write_bytes(out.read_bytes())
print(str(out))

