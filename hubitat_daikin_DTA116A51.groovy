/**
 * Daikin Modbus TCP — ON/OFF + Polling (frames estáticos)
 * Baseado no seu driver com conexão correta + tabela de comandos pronta.
 * - Usa apenas os frames hex já definidos (MBAP + PDU), sem montar comandos.
 * - Leitura de status: 1 registrador por AR (On/Off) — responde 00 00 (off) ou 00 01 (on).
 * - Escrita ON/OFF: 1 holding register por AR.
 * - Polling configurável em segundos.
 * 
 * Autor: Hernan + ChatGPT
 */

metadata {
    definition(name: "Daikin Modbus TCP", namespace: "TRATO", author: "VH") {
        capability "Initialize"
        capability "Refresh"
        capability "Actuator"
        capability "PushableButton"     

        command "acOn",  [[name:"AR (1-15)*", type:"NUMBER"]]
        command "acOff", [[name:"AR (1-15)*", type:"NUMBER"]]
        command "readStatus", [[name:"AR (1-15)*", type:"NUMBER"]]
        command "startPolling"
        command "stopPolling"
        command "setAcTemp", [[name:"AR (1-15)*", type:"NUMBER"], [name:"Temperatura (°C)*", type:"NUMBER"]]
        command "LigaAR1"
        command "DesligarAR1"        
        command "LigaAR2"
        command "DesligarAR2"        
        command "LigaAR3"
        command "DesligarAR3"        
        command "LigaAR4"
        command "DesligarAR4"        
        command "LigaAR5"
        command "DesligarAR5"        
        command "LigaAR6"
        command "DesligarAR6"        
        command "LigaAR7"
        command "DesligarAR7"        
        command "LigaAR8"
        command "DesligarAR8"        
        command "LigaAR9"
        command "DesligarAR9"        
        command "LigaAR10"
        command "DesligarAR10"        
        command "LigaAR11"
        command "DesligarAR11"        
        command "LigaAR12"
        command "DesligarAR12"        
        command "LigaAR13"
        command "DesligarAR13"        
        command "LigaAR14"
        command "DesligarAR14"        
        command "LigaAR15"
        command "DesligarAR15"        


        (1..15).each { n -> attribute "ac${n}Switch", "enum", ["on","off","unknown"] }
    }
}

preferences {
    input name: "ipAddress",     type: "text",   title: "Endereço IP", defaultValue: "192.168.0.100"
    input name: "port",          type: "number", title: "Porta TCP",   defaultValue: 502
    input name: "numberOfACs",   type: "number", title: "Quantidade de ARs (1-15)",  defaultValue: 15, range: "1..15"
    input name: "pollSeconds",   type: "number", title: "Intervalo de polling (3–300s)", defaultValue: 10, range: "3..300"
    input name: "logEnable",     type: "bool",   title: "Logs de depuração (DEBUG)", defaultValue: true
}

/* ==========================
   FRAMES ESTÁTICOS (HEX)
   ========================== */
import groovy.transform.Field


@Field static final Map<Integer,String> FRAME_ON = [
 1:"00 01 00 00 00 06 01 06 07 D0 00 01",  2:"00 01 00 00 00 06 01 06 07 D3 00 01",
 3:"00 01 00 00 00 06 01 06 07 D6 00 01",  4:"00 01 00 00 00 06 01 06 07 D9 00 01",
 5:"00 01 00 00 00 06 01 06 07 DC 00 01",  6:"00 01 00 00 00 06 01 06 07 DF 00 01",
 7:"00 01 00 00 00 06 01 06 07 E2 00 01",  8:"00 01 00 00 00 06 01 06 07 E5 00 01",
 9:"00 01 00 00 00 06 01 06 07 E8 00 01", 10:"00 01 00 00 00 06 01 06 07 EB 00 01",
11:"00 01 00 00 00 06 01 06 07 EE 00 01", 12:"00 01 00 00 00 06 01 06 07 F1 00 01",
13:"00 01 00 00 00 06 01 06 07 F4 00 01", 14:"00 01 00 00 00 06 01 06 07 F7 00 01",
15:"00 01 00 00 00 06 01 06 07 FA 00 01"
]

@Field static final Map<Integer,String> FRAME_OFF = [
 1:"00 01 00 00 00 06 01 06 07 D0 00 00",  2:"00 01 00 00 00 06 01 06 07 D3 00 00",
 3:"00 01 00 00 00 06 01 06 07 D6 00 00",  4:"00 01 00 00 00 06 01 06 07 D9 00 00",
 5:"00 01 00 00 00 06 01 06 07 DC 00 00",  6:"00 01 00 00 00 06 01 06 07 DF 00 00",
 7:"00 01 00 00 00 06 01 06 07 E2 00 00",  8:"00 01 00 00 00 06 01 06 07 E5 00 00",
 9:"00 01 00 00 00 06 01 06 07 E8 00 00", 10:"00 01 00 00 00 06 01 06 07 EB 00 00",
11:"00 01 00 00 00 06 01 06 07 EE 00 00", 12:"00 01 00 00 00 06 01 06 07 F1 00 00",
13:"00 01 00 00 00 06 01 06 07 F4 00 00", 14:"00 01 00 00 00 06 01 06 07 F7 00 00",
15:"00 01 00 00 00 06 01 06 07 FA 00 00"
]

@Field static final Map<Integer,String> FRAME_STATUS = [
 1:"00 01 00 00 00 06 01 04 07 D0 00 01",  2:"00 01 00 00 00 06 01 04 07 D6 00 01",
 3:"00 01 00 00 00 06 01 04 07 DC 00 01",  4:"00 01 00 00 00 06 01 04 07 E2 00 01",
 5:"00 01 00 00 00 06 01 04 07 E8 00 01",  6:"00 01 00 00 00 06 01 04 07 EE 00 01",
 7:"00 01 00 00 00 06 01 04 07 F4 00 01",  8:"00 01 00 00 00 06 01 04 07 FA 00 01",
 9:"00 01 00 00 00 06 01 04 08 00 00 01", 10:"00 01 00 00 00 06 01 04 08 06 00 01",
11:"00 01 00 00 00 06 01 04 08 0C 00 01", 12:"00 01 00 00 00 06 01 04 08 12 00 01",
13:"00 01 00 00 00 06 01 04 08 18 00 01", 14:"00 01 00 00 00 06 01 04 08 1E 00 01",
15:"00 01 00 00 00 06 01 04 08 24 00 01"
]

/* ==========================
   CICLO DE VIDA
   ========================== */

def installed() { initialize() }
def updated()   { initialize() }

def initialize() {
    unschedule()
    state.pending = null       // último AR/tipo enviado (para mapear resposta)
    state.sending = false
    (1..15).each { sendEvent(name:"ac${it}Switch", value:"unknown") }
    if (logEnable) log.debug "Inicializado: ${ipAddress}:${port}, polling=${pollSeconds}s, ACs=${numberOfACs}"
    startPolling()
}

def refresh() { pollAll() }

/* ==========================
   CONEXÃO RAW SOCKET
   ========================== */

private boolean connectSocket() {

   def ip = ipAddress
    def port1 = port?.toInteger() ?: 80
    
    
    try {
        interfaces.rawSocket.connect(ip, port1, byteInterface: true)
        if (logEnable) log.debug "Socket conectado"
        return true
    } catch (e) {
        log.error "Erro ao conectar: ${e}"
        return false
    }
}
private void closeSocket() {
    try {
        interfaces.rawSocket.close()
        if (logEnable) log.debug "Socket fechado"
    } catch (e) {
        if (logEnable) log.warn "Erro ao fechar: ${e.message}"
    }
}

/* ======== COMANDOS MANUAIS ===== 
   ========================== */

def LigaAR1( ){
acOn(1)
}

def DesligarAR1(){
acOff(1)    
}

def LigaAR2( ){
acOn(2)
}

def DesligarAR2(){
acOff(2)    
}

def LigaAR3( ){
acOn(3)
}

def DesligarAR3(){
acOff(3)    
}

def LigaAR4( ){
acOn(4)
}

def DesligarAR4(){
acOff(4)    
}

def LigaAR5( ){
acOn(5)
}

def DesligarAR5(){
acOff(5)    
}

def LigaAR6( ){
acOn(6)
}

def DesligarAR6(){
acOff(6)    
}

def LigaAR7( ){
acOn(7)
}

def DesligarAR7(){
acOff(7)    
}

def LigaAR8( ){
acOn(8)
}

def DesligarAR8(){
acOff(8)    
}

def LigaAR9( ){
acOn(9)
}

def DesligarAR9(){
acOff(9)    
}

def LigaAR10( ){
acOn(10)
}

def DesligarAR10(){
acOff(10)    
}

def LigaAR11( ){
acOn(11)
}

def DesligarAR11(){
acOff(11)    
}

def LigaAR12( ){
acOn(12)
}

def DesligarAR12(){
acOff(12)    
}

def LigaAR13( ){
acOn(13)
}

def DesligarAR13(){
acOff(13)    
}

def LigaAR14( ){
acOn(14)
}

def DesligarAR14(){
acOff(14)    
}

def LigaAR15( ){
acOn(15)
}

def DesligarAR15(){
acOff(15)    
}


def push(pushed) {
	log.debug "push: button = ${pushed}"
	switch(pushed) {
    case "11" : LigaAR1(); break
    case "10" : DesligarAR1(); break
    case "21" : LigaAR2(); break
    case "20" : DesligarAR2(); break
    case "31" : LigaAR3(); break
    case "30" : DesligarAR3(); break
    case "41" : LigaAR4(); break
    case "40" : DesligarAR4(); break
    case "51" : LigaAR5(); break
    case "50" : DesligarAR5(); break
    case "61" : LigaAR6(); break
    case "60" : DesligarAR6(); break
    case "71" : LigaAR7(); break
    case "70" : DesligarAR7(); break
    case "81" : LigaAR8(); break
    case "80" : DesligarAR8(); break
    case "91" : LigaAR9(); break
    case "90" : DesligarAR9(); break
    case "101" : LigaAR10(); break
    case "100" : DesligarAR10(); break
    case "111" : LigaAR11(); break
    case "110" : DesligarAR11(); break
    case "121" : LigaAR12(); break
    case "120" : DesligarAR12(); break
    case "131" : LigaAR13(); break
    case "130" : DesligarAR13(); break
    case "141" : LigaAR14(); break
    case "140" : DesligarAR14(); break
    case "151" : LigaAR15(); break
    case "150" : DesligarAR15(); break

default:
			"${pushed}"()
            //logDebug("push: Botão inválido.")
			break
	}        
}        

/* ==========================
   ENVIO USANDO FRAMES PRONTOS
   ========================== */

private static String cleanHex(String s) { s?.replaceAll(/[^0-9A-Fa-f]/, "") ?: "" }

private void sendFrameHex(String hex, Integer ac, String kind) {
    String cleaned = cleanHex(hex)
    
    if (!cleaned) { log.warn "Frame vazio p/ AC ${ac} (${kind})"; return }
    byte[] payload = new byte[cleaned.length()/2]
    for (int i=0; i<cleaned.length(); i+=2) {
        payload[i/2] = (byte) Integer.parseInt(cleaned.substring(i, i+2), 16)
    }
    if (!connectSocket()) return
    state.pending = [ac: ac, kind: kind, ts: now()]
    if (logEnable) log.debug "Envio de  (${kind}) AC ${ac}: ${cleaned.toUpperCase().toList().collate(2).collect{it.join()}.join(' ')}"  
  
    valorhex2 = cleaned.toUpperCase().toList().collate(2).collect{it.join()}.join(' ')    
    log.debug "Valor HEX enviado " + valorhex2
    interfaces.rawSocket.sendMessage(valorhex2)
    // pequena pausa para garantir processamento no destino
    pauseExecution(70)
    //closeSocket()
}

/* ==========================
   COMANDOS PÚBLICOS
   ========================== */

def acOn(acNum)  { Integer n=(acNum as int); sendFrameHex(FRAME_ON[n], n, "AC-on") }
def acOff(acNum) { Integer n=(acNum as int); sendFrameHex(FRAME_OFF[n], n, "AC-off") }
def readStatus(acNum=1) { Integer n=(acNum as int); sendFrameHex(FRAME_STATUS[n], n, "Leitura Status do AR") }

/** Poll de todos os ARs com espaçamento entre envios (sequencial) */
def pollAll() {
    Integer n = (settings?.numberOfACs ?: 15) as int
    n = Math.max(1, Math.min(15, n))
    // Envia sequencialmente com pequenos delays (runInMillis encadeado)
    scheduleStatusBurst(1, n)
    // Rearma próximo ciclo
    Integer sec = (settings?.pollSeconds ?: 10) as int
    sec = Math.max(3, Math.min(300, sec))
    runIn(sec, "pollAll", [overwrite:true])
}

private void scheduleStatusBurst(Integer current, Integer max) {
    if (current > max) return
    runInMillis(1, "___sendStatusOne", [data:[ac:current, max:max]])
}
def ___sendStatusOne(Map data) {
    Integer ac = (data?.ac ?: 1) as int
    Integer max = (data?.max ?: 1) as int
    readStatus(ac)
    if (ac < max) runInMillis(120, "___sendStatusOne", [data:[ac:ac+1, max:max]])
}

def startPolling() {
    unschedule()
    if (logEnable) log.debug "Iniciando polling a cada ${pollSeconds}s"
    pollAll()
}
def stopPolling() {
    unschedule()
    if (logEnable) log.debug "Polling parado"
}
def setPollingSeconds(Integer s) {
    device.updateSetting("pollSeconds", [value: s.toString(), type: "number"])
    if (logEnable) log.debug "Novo intervalo de polling: ${s}s"
    startPolling()
}

/* ==========================
   PARSE DA RESPOSTA
   ========================== */

/* ==========================
   PARSE DA RESPOSTA
   ========================== */

private static String _toHex2(int b) { String.format("%02X", b & 0xFF) }
private static List<Integer> _hexToBytes(String s) {
    String h = s?.replaceAll(/[^0-9A-Fa-f]/,'') ?: ''
    if (h.length() < 2) return []
    (0..<h.length()/2).collect { Integer.parseInt(h.substring(it*2, it*2+2), 16) }
}

def parse(String message) {
    if (logEnable) log.debug "RX raw: ${message}"

    // Converte string recebida para bytes
    List<Integer> b = _hexToBytes(message)
    if (b.size() < 9) {  // MBAP(7) + pelo menos FC e ByteCount/Exception
        log.warn "Resposta muito curta (${b.size()} bytes). Ignorada."
        return
    }

    // --- Cabeçalho MBAP ---
    int transHi = b[0], transLo = b[1]
    int protoHi = b[2], protoLo = b[3]
    int lenHi   = b[4], lenLo   = b[5]
    int unitId  = b[6] & 0xFF   // normalmente 0x01

    // --- PDU ---
    int fc = b[7] & 0xFF

    // Trata exceção (fc + 0x80)
    if ((fc & 0x80) != 0) {
        int exc = b[8] & 0xFF
        log.warn "Modbus exception 0x${_toHex2(exc)} para requisição pendente ${state?.pending}"
        return
    }

    switch (fc) {
        case 0x04: // Read Input Registers
            int byteCount = b[8] & 0xFF
            if (byteCount < 2 || b.size() < 9 + byteCount) {
                log.warn "ByteCount inválido (${byteCount}) ou frame truncado."
                return
            }
            // Lemos 1 registrador => 2 bytes
            int hi = b[9]  & 0xFF
            int lo = b[10] & 0xFF
            int regVal = (hi << 8) | lo

        if (state?.pending?.kind == "getOpForSetTemp") {
    int n = state?.pending?.ac as int
    int mode   = regVal & 0xFF          // 0..7 (Fan/Heat/Cool/Auto/...)
    int opAuto = (mode == 3) ? 2 : 0    // em Auto selecione Cooling(2); caso contrário 0 (don't care)

    int hold42002 = ((opAuto & 0xFF) << 8) | (mode & 0xFF)
    // escreve 42002 "limpo": [opAuto<<8 | mode], sem copiar filter sign/status da leitura
    sendFrameHex(_writeSingleRegHex(_addrHoldOpMode(n), hold42002), n, "syncHoldOpMode")

    // pequena folga antes do setpoint (não é o mesmo registrador, mas dá estabilidade)
    runInMillis(700, "___writeSetpointAfterSync", [data:[ac:n]])

    if (logEnable) log.debug "AC ${n}: 32002=0x${String.format('%04X', regVal)} -> 42002=0x${String.format('%04X', hold42002)} (mode=${mode}, opAuto=${opAuto})"
    return
}
        
            boolean isOn = (regVal & 0x0001) == 0x0001  // bit0 = On/Off
            Integer ac = (state?.pending?.ac as Integer) ?: null
            String origin = (state?.pending?.kind ?: "desconhecido")

            if (ac) {
                String val = isOn ? "on" : "off"
                sendEvent(name: "ac${ac}Switch", value: val)
                if (logEnable) log.debug "AC ${ac} status => ${val} (reg=0x${String.format('%04X', regVal)} | origem=${origin})"
            } else {
                if (logEnable) log.debug "Status recebido sem AC pendente (reg=0x${String.format('%04X', regVal)} on=${isOn})"
            }
            break

        case 0x06: // Preset Single Register (ACK de escrita)
        case 0x10: // Preset Multiple Registers (ACK de escrita)
            if (logEnable) log.debug "ACK escrita Modbus (fc=0x${_toHex2(fc)}) pendente=${state?.pending}"
            break

        default:
            log.warn "Função Modbus não tratada: 0x${_toHex2(fc)}"
    }
}

/* ========= HELPERS NOVOS ========= */

// Endereços base-0 por AR
private static int _addrInOpMode(int ac){ return 0x07D1 + ((ac-1)*6) } // 32002, 32008, ...
private static int _addrHoldOpMode(int ac){ return 0x07D1 + ((ac-1)*3) } // 42002, 42005, ...
private static int _addrSetpoint(int ac){ return 0x07D0 + ((ac-1)*3) + 2 } // 42003, 42006, ...

// Frames Modbus-TCP (MBAP len=0x0006)
private static String _readSingleRegHex(int addr){
    return String.format("00 01 00 00 00 06 01 04 %02X %02X 00 01", (addr>>8)&0xFF, addr&0xFF)
}
private static String _writeSingleRegHex(int addr, int val16){
    return String.format("00 01 00 00 00 06 01 06 %02X %02X %02X %02X",
        (addr>>8)&0xFF, addr&0xFF, (val16>>8)&0xFF, val16&0xFF)
}

/* ========= SET TEMP COM PRÉ-SYNC ========= */

def setAcTemp(acNum, tempC){
    Integer n = (acNum as int)
    if (n<1 || n>15) { log.warn "AC inválido: ${acNum}"; return }

    // converte °C -> décimos (signed 16-bit)
    BigDecimal t = (tempC as BigDecimal)
    int v = Math.round(t * 10G); v = Math.max(-1279, Math.min(1279, v))

    // guarda alvo para o Passo C
    state._pendingSetTemp = [ac:n, val:v, txt:"${t}°C"]

    // Passo A: ler 32002 (op. mode/status) para B
    int aIn = _addrInOpMode(n)
    sendFrameHex(_readSingleRegHex(aIn), n, "getOpForSetTemp")
}

/* ========= CALLBACK PARA ENVIAR 42003 APÓS SYNC ========= */

def ___writeSetpointAfterSync(Map data){
    Integer n = (data?.ac as int) ?: state?._pendingSetTemp?.ac
    def pend = state?._pendingSetTemp
    if (!n || !pend){ if (logEnable) log.warn "Sem alvo p/ setpoint"; return }
    String hex = _writeSingleRegHex(_addrSetpoint(n), pend.val)
    sendFrameHex(hex, n, "setTemp ${pend.txt}")
}

/* ========= AJUSTE NO parse() EXISTENTE — apenas adiciona um ramo ========= */
// Dentro do seu switch(fc) { case 0x04: ... } após calcular regVal:

// Se esta leitura foi o Passo A (getOpForSetTemp):
if (state?.pending?.kind == "getOpForSetTemp") {
    int n = state?.pending?.ac as int
    int regVal = ((b[9]&0xFF)<<8) | (b[10]&0xFF)  // já calculado no seu parse

    // Se modo=Auto (valor 3 no byte baixo), garanta OperationStatus=1 ou 2 no byte alto
    int opMode = regVal & 0x00FF
    int opStat = (regVal >> 8) & 0x00FF
    if (opMode == 3 && opStat == 0) opStat = 1   // default: Heating (1) se vier "don't care"
    int hold42002 = (opStat<<8) | opMode

    // Passo B: espelha B -> 42002
    sendFrameHex(_writeSingleRegHex(_addrHoldOpMode(n), hold42002), n, "syncHoldOpMode")

    // Passo C: agenda 600 ms depois o 42003 (boa prática, ainda que não seja o mesmo registrador)
    runInMillis(600, "___writeSetpointAfterSync", [data:[ac:n]])
    return
}
