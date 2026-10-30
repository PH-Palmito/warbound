from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "relatorio" / "Warbound_Entrega_1.docx"
ASSETS = ROOT / "tmp" / "entrega1_docx_assets"


def font(size, bold=False):
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
    ]
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default()


def draw_box(draw, box, text, fill, border=(46, 64, 83), text_fill=(255, 255, 255), size=25):
    draw.rounded_rectangle(box, radius=16, fill=fill, outline=border, width=3)
    f = font(size, True)
    left, top, right, bottom = box
    lines = text.split("\n")
    line_height = int(size * 1.25)
    y = (top + bottom - line_height * len(lines)) // 2
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=f)
        x = (left + right - (bbox[2] - bbox[0])) // 2
        draw.text((x, y), line, font=f, fill=text_fill)
        y += line_height


def arrow(draw, start, end, label=None, label_at=None):
    draw.line([start, end], fill=(46, 64, 83), width=4)
    x1, y1 = start
    x2, y2 = end
    if abs(x2 - x1) >= abs(y2 - y1):
        points = [(x2, y2), (x2 - 14 if x2 > x1 else x2 + 14, y2 - 8), (x2 - 14 if x2 > x1 else x2 + 14, y2 + 8)]
    else:
        points = [(x2, y2), (x2 - 8, y2 - 14 if y2 > y1 else y2 + 14), (x2 + 8, y2 - 14 if y2 > y1 else y2 + 14)]
    draw.polygon(points, fill=(46, 64, 83))
    if label:
        f = font(18)
        lx, ly = label_at if label_at else ((x1 + x2) // 2, (y1 + y2) // 2 - 22)
        draw.text((lx, ly), label, font=f, fill=(25, 38, 54))


def make_architecture_diagram(path):
    img = Image.new("RGB", (1800, 860), (255, 255, 255))
    draw = ImageDraw.Draw(img)
    title_font = font(34, True)
    draw.text((55, 30), "Arquitetura cliente-servidor do Warbound", font=title_font, fill=(20, 35, 53))
    draw.rounded_rectangle((35, 105, 635, 760), radius=22, outline=(110, 130, 150), width=3)
    draw.text((55, 125), "Clientes JavaFX", font=font(26, True), fill=(20, 35, 53))
    draw_box(draw, (75, 205, 325, 315), "Cliente A\nUI e controle", (44, 109, 144), size=22)
    draw_box(draw, (355, 205, 605, 315), "Cliente B\nUI e controle", (44, 109, 144), size=22)
    draw_box(draw, (190, 465, 475, 575), "Stub RMI\nMiddleware cliente", (49, 130, 130), size=22)
    arrow(draw, (200, 315), (285, 465))
    arrow(draw, (480, 315), (385, 465))
    draw_box(draw, (715, 335, 995, 450), "RMI Registry\nporta 1099", (112, 98, 158), size=23)
    arrow(draw, (475, 510), (715, 395), "lookup inicial", (515, 420))
    draw.rounded_rectangle((1090, 105, 1765, 760), radius=22, outline=(110, 130, 150), width=3)
    draw.text((1110, 125), "Servidor Warbound", font=font(26, True), fill=(20, 35, 53))
    draw_box(draw, (1140, 230, 1390, 340), "Runtime RMI\nSkeleton", (184, 103, 44), size=22)
    draw_box(draw, (1450, 230, 1710, 340), "WarboundService\nobjeto remoto", (184, 103, 44), size=21)
    draw_box(draw, (1140, 480, 1390, 590), "Gerenciador\nde partidas", (153, 79, 92), size=22)
    draw_box(draw, (1450, 480, 1710, 590), "Motor de regras\ne estado", (153, 79, 92), size=21)
    arrow(draw, (475, 520), (1140, 285), "TCP/RMI - porta 5000", (780, 290))
    arrow(draw, (1390, 285), (1450, 285))
    arrow(draw, (1580, 340), (1580, 480))
    arrow(draw, (1450, 535), (1390, 535))
    draw.text((55, 798), "O Registry é utilizado somente para localizar o serviço remoto; as jogadas seguem diretamente ao servidor.", font=font(20), fill=(56, 70, 85))
    img.save(path)


def make_sequence_diagram(path):
    img = Image.new("RGB", (1600, 1080), (255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.text((55, 30), "Sequência de uma jogada", font=font(34, True), fill=(20, 35, 53))
    actors = [(145, "Cliente ativo"), (450, "Stub RMI"), (790, "Runtime RMI\nSkeleton"), (1130, "Servidor"), (1460, "Estado")]
    for x, label in actors:
        draw_box(draw, (x - 95, 100, x + 95, 180), label, (44, 109, 144), size=19)
        draw.line((x, 180, x, 1000), fill=(135, 149, 163), width=2)
    events = [
        (245, 145, 450, "1. realizarJogada(comando)"),
        (330, 450, 790, "2. serializa e envia via TCP"),
        (415, 790, 1130, "3. desserializa e encaminha"),
        (500, 1130, 1460, "4. valida e atualiza estado"),
        (610, 1460, 1130, "5. estado atualizado"),
        (700, 1130, 790, "6. ResultadoJogada"),
        (790, 790, 450, "7. serializa retorno"),
        (880, 450, 145, "8. atualiza a interface"),
    ]
    for y, x1, x2, label in events:
        arrow(draw, (x1, y), (x2, y), label, ((min(x1, x2) + max(x1, x2)) // 2 - 105, y - 28))
    draw.rounded_rectangle((130, 955, 1470, 1035), radius=12, fill=(239, 244, 248), outline=(180, 195, 208), width=2)
    draw.text((155, 980), "O cliente adversário consulta obterEstado(partidaId) por polling e recebe somente sua visão permitida da partida.", font=font(20), fill=(25, 38, 54))
    img.save(path)


def set_cell_shading(cell, color):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), color)
    tc_pr.append(shd)


def set_cell_margins(cell, top=110, start=110, bottom=110, end=110):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for side, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{side}"))
        if node is None:
            node = OxmlElement(f"w:{side}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_repeat_table_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def add_page_number(paragraph):
    run = paragraph.add_run()
    fld_char1 = OxmlElement("w:fldChar")
    fld_char1.set(qn("w:fldCharType"), "begin")
    instr_text = OxmlElement("w:instrText")
    instr_text.set(qn("xml:space"), "preserve")
    instr_text.text = "PAGE"
    fld_char2 = OxmlElement("w:fldChar")
    fld_char2.set(qn("w:fldCharType"), "end")
    run._r.append(fld_char1)
    run._r.append(instr_text)
    run._r.append(fld_char2)


def set_run_font(run, name="Times New Roman", size=12, bold=None, italic=None):
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:ascii"), name)
    run._element.rPr.rFonts.set(qn("w:hAnsi"), name)
    run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic


def para(doc, text="", align=WD_ALIGN_PARAGRAPH.JUSTIFY, first_indent=True, space_after=0, space_before=0, line_spacing=1.5, bold_lead=None):
    p = doc.add_paragraph()
    p.alignment = align
    fmt = p.paragraph_format
    fmt.line_spacing = line_spacing
    fmt.space_after = Pt(space_after)
    fmt.space_before = Pt(space_before)
    if first_indent:
        fmt.first_line_indent = Cm(1.25)
    run = p.add_run(text)
    set_run_font(run)
    if bold_lead and text.startswith(bold_lead):
        run.clear()
        lead = p.add_run(bold_lead)
        set_run_font(lead, bold=True)
        rest = p.add_run(text[len(bold_lead):])
        set_run_font(rest)
    return p


def heading(doc, text, level=1):
    p = doc.add_paragraph()
    p.style = f"Heading {level}"
    p.paragraph_format.keep_with_next = True
    p.paragraph_format.space_before = Pt(14 if level == 1 else 8)
    p.paragraph_format.space_after = Pt(6)
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    run = p.add_run(text)
    set_run_font(run, size=12, bold=True)
    return p


def caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(6)
    run = p.add_run(text)
    set_run_font(run, size=10)
    return p


def add_component_table(doc):
    rows = [
        ("Cliente JavaFX", "Apresenta mão, tabuleiro, recursos, turno e mensagens ao jogador."),
        ("Controlador", "Transforma interações da interface em comandos de jogada."),
        ("Stub RMI", "Proxy local que encaminha chamadas remotas para o serviço do servidor."),
        ("RMI Registry", "Registra o serviço e devolve sua referência remota na conexão inicial."),
        ("Runtime RMI e Skeleton", "Recebe a chamada, realiza o unmarshalling e a encaminha ao objeto remoto."),
        ("WarboundService", "Expõe as operações remotas e coordena o acesso às partidas."),
        ("Motor de regras e estado", "Valida as ações e mantém o estado autoritativo de cada partida."),
    ]
    table = doc.add_table(rows=1, cols=2)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    table.rows[0].cells[0].width = Cm(5.1)
    table.rows[0].cells[1].width = Cm(10.6)
    for cell, text in zip(table.rows[0].cells, ("Componente", "Responsabilidade")):
        cell.text = text
        set_cell_shading(cell, "1F4E78")
        set_cell_margins(cell)
        cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        for run in cell.paragraphs[0].runs:
            set_run_font(run, size=10, bold=True)
            run.font.color.rgb = RGBColor(255, 255, 255)
    set_repeat_table_header(table.rows[0])
    for index, (name, responsibility) in enumerate(rows):
        cells = table.add_row().cells
        for cell, text in zip(cells, (name, responsibility)):
            cell.text = text
            set_cell_margins(cell)
            cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
            if index % 2 == 1:
                set_cell_shading(cell, "EEF4F8")
            for p in cell.paragraphs:
                p.paragraph_format.space_after = Pt(0)
                p.paragraph_format.line_spacing = 1.0
                for run in p.runs:
                    set_run_font(run, size=10)
    caption(doc, "Quadro 1 - Componentes da arquitetura da solução")
    caption(doc, "Fonte: Elaborado pelos autores (2026).")


def build():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)
    architecture = ASSETS / "arquitetura.png"
    sequence = ASSETS / "sequencia.png"
    make_architecture_diagram(architecture)
    make_sequence_diagram(sequence)

    doc = Document()
    section = doc.sections[0]
    section.page_width = Cm(21)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(3)
    section.left_margin = Cm(3)
    section.bottom_margin = Cm(2)
    section.right_margin = Cm(2)

    normal = doc.styles["Normal"]
    normal.font.name = "Times New Roman"
    normal._element.rPr.rFonts.set(qn("w:ascii"), "Times New Roman")
    normal._element.rPr.rFonts.set(qn("w:hAnsi"), "Times New Roman")
    normal.font.size = Pt(12)
    for level in (1, 2):
        style = doc.styles[f"Heading {level}"]
        style.font.name = "Times New Roman"
        style._element.rPr.rFonts.set(qn("w:ascii"), "Times New Roman")
        style._element.rPr.rFonts.set(qn("w:hAnsi"), "Times New Roman")
        style.font.size = Pt(12)
        style.font.bold = True
        style.font.color.rgb = RGBColor(0, 0, 0)

    footer = section.footer
    footer_p = footer.paragraphs[0]
    footer_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    add_page_number(footer_p)
    for run in footer_p.runs:
        set_run_font(run, size=10)

    # Cover
    for _ in range(5):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("DISCIPLINA: SISTEMAS DISTRIBUÍDOS")
    set_run_font(r, size=12, bold=True)
    for _ in range(7):
        doc.add_paragraph()
    for name in ("EMANOEL MENDES CERQUEIRA DOS SANTOS", "PEDRO HENRIQUE BISPO PALMITO"):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(name)
        set_run_font(r, size=12)
    for _ in range(8):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("WARBOUND")
    set_run_font(r, size=14, bold=True)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("JOGO MULTIJOGADOR COM JAVA RMI")
    set_run_font(r, size=14, bold=True)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("ENTREGA 1 - INTRODUÇÃO E ARQUITETURA")
    set_run_font(r, size=12)
    for _ in range(8):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("2026")
    set_run_font(r, size=12)
    doc.add_page_break()

    # Title page
    for name in ("EMANOEL MENDES CERQUEIRA DOS SANTOS", "PEDRO HENRIQUE BISPO PALMITO"):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(name)
        set_run_font(r, size=12)
    for _ in range(7):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("WARBOUND")
    set_run_font(r, size=14, bold=True)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("JOGO MULTIJOGADOR COM JAVA RMI")
    set_run_font(r, size=14, bold=True)
    for _ in range(5):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Cm(8)
    p.paragraph_format.line_spacing = 1.0
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    r = p.add_run("Relatório técnico apresentado à disciplina de Sistemas Distribuídos como requisito parcial de avaliação. Este documento corresponde à primeira entrega do projeto e apresenta a definição do jogo e a arquitetura distribuída proposta.")
    set_run_font(r, size=12)
    for _ in range(12):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("2026")
    set_run_font(r, size=12)
    doc.add_page_break()

    # Resumo
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("RESUMO")
    set_run_font(r, size=12, bold=True)
    resumo = ("Este relatório apresenta a primeira etapa do projeto Warbound, um jogo de cartas multiplayer "
              "baseado em civilizações históricas, como Romanos, Samurais e Vikings. O objetivo do jogo é "
              "reduzir os pontos de vida do comandante adversário por meio de unidades, táticas e decisões "
              "estratégicas realizadas em turnos. A solução adota Java Remote Method Invocation, Java RMI, "
              "como mecanismo de comunicação distribuída. A escolha decorre do caráter orientado a objetos "
              "do domínio do jogo e da utilização de Java tanto no cliente quanto no servidor. A arquitetura "
              "proposta concentra o estado oficial das partidas em um servidor central, que recebe comandos "
              "dos clientes, valida as regras e devolve uma visão atualizada do jogo. O RMI Registry permite a "
              "localização inicial do serviço remoto, enquanto os stubs e o runtime RMI tratam a serialização e "
              "o transporte das chamadas. A proposta também prevê sincronização por partida e atualização do "
              "cliente adversário por polling, medidas que preservam a consistência do estado e limitam a exposição "
              "de informações privadas, como as cartas da mão de cada jogador.")
    para(doc, resumo, first_indent=False, line_spacing=1.0)
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(10)
    r = p.add_run("Palavras-chave: sistemas distribuídos; Java RMI; jogos multijogador; concorrência; card game.")
    set_run_font(r, size=12, bold=True)
    doc.add_page_break()

    # Lists and TOC
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("LISTA DE ILUSTRAÇÕES")
    set_run_font(r, size=12, bold=True)
    for item in ("Figura 1 - Diagrama da arquitetura da solução", "Figura 2 - Diagrama de sequência de uma jogada", "Quadro 1 - Componentes da arquitetura da solução"):
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Cm(1.25)
        r = p.add_run(item)
        set_run_font(r, size=12)
    doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("SUMÁRIO")
    set_run_font(r, size=12, bold=True)
    toc_items = [
        "1 INTRODUÇÃO",
        "1.1 DESCRIÇÃO DO JOGO",
        "1.2 MOTIVAÇÃO DA ESCOLHA DO PROTOCOLO",
        "2 ARQUITETURA DA SOLUÇÃO",
        "2.1 DIAGRAMA DA ARQUITETURA",
        "2.2 FLUXO DE MENSAGENS A CADA JOGADA",
        "REFERÊNCIAS",
    ]
    for item in toc_items:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Cm(1.25 if item[1] != "." else 2.0)
        r = p.add_run(item)
        set_run_font(r, size=12)
    doc.add_page_break()

    heading(doc, "1 INTRODUÇÃO")
    para(doc, "O projeto Warbound é desenvolvido no contexto da disciplina de Sistemas Distribuídos para aplicar conceitos de comunicação entre processos, invocação remota, estado compartilhado e concorrência. Esta primeira entrega define o jogo e apresenta a arquitetura que orientará sua implementação. A proposta separa a interface gráfica do servidor central, garantindo que as regras e o estado de cada partida sejam controlados em um único ponto confiável.")
    heading(doc, "1.1 DESCRIÇÃO DO JOGO", level=2)
    para(doc, "Warbound é um card game estratégico para dois jogadores. Cada participante escolhe uma civilização histórica, inicialmente Romana, Samurai ou Viking, e utiliza um baralho temático composto por unidades, táticas e líderes. As civilizações se diferenciam pela identidade visual, pelos efeitos de suas cartas e pelas estratégias que incentivam durante a partida.")
    para(doc, "Cada jogador controla um comandante com 20 pontos de vida. No seu turno, recebe recursos, joga cartas de unidade ou tática, declara ataques, escolhe alvos e encerra o turno. A condição principal de vitória é reduzir a vida do comandante adversário a zero. Uma jogada corresponde a uma intenção enviada pelo cliente, como baixar uma carta, atacar uma unidade, atacar o comandante ou encerrar o turno.")
    para(doc, "A implementação mantém o servidor como autoridade da partida. Portanto, o cliente não altera diretamente a vida, os recursos, as cartas ou a vez de jogar: ele solicita a ação e exibe apenas o resultado validado pelo servidor. Essa decisão impede que uma interface alterada ou duas requisições concorrentes comprometam o estado do jogo.")
    heading(doc, "1.2 MOTIVAÇÃO DA ESCOLHA DO PROTOCOLO", level=2)
    para(doc, "O protocolo selecionado é o Java RMI. A escolha é coerente com um domínio composto por objetos como Partida, Jogador, Carta, Baralho, Jogada e EstadoPartida. O RMI permite a invocação de métodos de objetos localizados em outra máquina virtual Java e utiliza serialização para realizar o marshalling dos parâmetros e o unmarshalling dos resultados (ORACLE, 2025a).")
    para(doc, "O uso de Java tanto no cliente JavaFX quanto no servidor torna o RMI uma alternativa direta e didática. A interface remota concentra as operações permitidas pelo jogo, enquanto o RMI Registry permite que o cliente encontre a referência do serviço no início da conexão. O modelo também torna explícitos os papéis de stub, como representante local do objeto remoto, e de skeleton ou runtime do servidor, que recebe a chamada e a encaminha para a implementação remota (ORACLE, 2025b).")

    heading(doc, "2 ARQUITETURA DA SOLUÇÃO")
    para(doc, "A arquitetura proposta segue o modelo cliente-servidor. O cliente é responsável por receber a interação do jogador e exibir uma visão da partida; o servidor hospeda as partidas, aplica as regras e conserva o estado autoritativo. A comunicação ocorre por Java RMI, com o Registry na porta 1099 para descoberta do serviço e com o objeto remoto exportado na porta 5000 para as operações da partida.")
    heading(doc, "2.1 DIAGRAMA DA ARQUITETURA", level=2)
    para(doc, "Conforme a Figura 1, cada cliente possui uma interface JavaFX, um controlador e uma referência Stub RMI. Após localizar o WarboundService no RMI Registry, os clientes invocam diretamente o objeto remoto hospedado pelo servidor. No lado servidor, o runtime RMI desempenha o papel de skeleton, encaminhando a chamada ao serviço, que utiliza o gerenciador de partidas e o motor de regras para consultar ou alterar o estado oficial.")
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(str(architecture), width=Cm(15.6))
    caption(doc, "Figura 1 - Diagrama da arquitetura da solução")
    caption(doc, "Fonte: Elaborado pelos autores (2026).")
    add_component_table(doc)

    heading(doc, "2.2 FLUXO DE MENSAGENS A CADA JOGADA", level=2)
    para(doc, "A Figura 2 apresenta o caminho de uma jogada. Primeiro, o usuário seleciona uma ação na interface. O controlador monta um ComandoJogada com o identificador da partida, o jogador, a ação desejada, a carta ou o alvo envolvido e a versão conhecida do estado. O Stub RMI serializa esse comando e o envia pela rede ao servidor.")
    para(doc, "No servidor, o runtime RMI desserializa o comando e o encaminha ao WarboundService. O serviço identifica a partida, sincroniza o acesso ao estado e solicita a validação ao motor de regras. Se a ação for válida, o servidor aplica a alteração e devolve um ResultadoJogada; se for inválida, devolve uma exceção ou resultado de erro. A resposta é serializada e retorna ao cliente responsável pela jogada.")
    para(doc, "O jogador adversário atualiza sua tela por polling, consultando o estado da partida. A resposta contém apenas a visão autorizada para aquele jogador: por exemplo, informações públicas do adversário e a mão privada do próprio jogador. A primeira versão utiliza polling por ser simples de implementar e depurar; callbacks RMI podem ser avaliados em uma evolução posterior.")
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(str(sequence), width=Cm(15.6))
    caption(doc, "Figura 2 - Diagrama de sequência de uma jogada")
    caption(doc, "Fonte: Elaborado pelos autores (2026).")

    heading(doc, "REFERÊNCIAS")
    reference1 = "ORACLE. Java Remote Method Invocation API Guide: Release 17. Disponível em: https://docs.oracle.com/en/java/javase/17/rmi/. Acesso em: 30 out. 2026."
    reference2 = "ORACLE. Java Remote Method Invocation: RMI System Overview. Java SE 17. Disponível em: https://docs.oracle.com/en/java/javase/17/docs/specs/rmi/arch.html. Acesso em: 30 out. 2026."
    for reference in (reference1, reference2):
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(8)
        p.paragraph_format.line_spacing = 1.0
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r = p.add_run(reference)
        set_run_font(r, size=12)

    doc.core_properties.title = "Warbound Jogo Multijogador com Java RMI"
    doc.core_properties.author = "Emanoel Mendes Cerqueira dos Santos; Pedro Henrique Bispo Palmito"
    doc.core_properties.subject = "Entrega 1 - Introdução e Arquitetura"
    doc.save(OUT)
    print(OUT)


if __name__ == "__main__":
    build()
