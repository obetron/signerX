package com.gelecex.signerx.smartcard;

/**
 * Created by obetron on 27.04.2022
 */

import com.gelecex.signerx.common.EnumOsArch;
import com.gelecex.signerx.common.EnumOsName;
import com.gelecex.signerx.common.exception.SignerxException;
import com.gelecex.signerx.common.smartcard.SmartcardAtr;
import com.gelecex.signerx.common.smartcard.SmartcardLibrary;
import com.gelecex.signerx.common.smartcard.SmartcardType;
import com.gelecex.signerx.utils.SCXmlParser;
import com.gelecex.signerx.utils.SignerxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.*;
import java.util.ArrayList;
import java.util.List;

public class SmartcardManagerImpl implements SmartcardManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartcardManagerImpl.class);

    @Override
    public void detectSmartcards() throws SignerxException {

    }

    private List<String> getAtrFromSmartcards() throws SignerxException {
        try {
            List<String> smartcardAtrList = new ArrayList<>();
            TerminalFactory terminalFactory = TerminalFactory.getDefault();
            CardTerminals terminals = terminalFactory.terminals();
            List<CardTerminal> cardTerminalList = terminals.list();
            for (CardTerminal cardTerminal : cardTerminalList) {
                Card card = cardTerminal.connect("T0");
                ATR atr = card.getATR();
                byte[] atrBytes = atr.getBytes();
                smartcardAtrList.add(SignerxUtils.byteToHex(atrBytes));
            }
            return smartcardAtrList;
        } catch (CardException e) {
            throw new SignerxException("Terminal listesi alinirken hata olustu!", e);
        }
    }

    private String detectSmartcardLib(String atrValue) throws SignerxException {
        List<SmartcardLibrary> smartcardLibraryList = getSmartcardLibraryList(atrValue);
        if(smartcardLibraryList.size() > 0) {
            for (SmartcardLibrary smartcardLibrary : smartcardLibraryList) {
                if(EnumOsArch.x32.toString().equalsIgnoreCase(smartcardLibrary.getArch())) {
                    return smartcardLibrary.getName();
                } else if(EnumOsArch.x64.toString().equalsIgnoreCase(smartcardLibrary.getArch())) {
                    return smartcardLibrary.getName();
                }
            }
        } else {
            return smartcardLibraryList.get(0).getName();
        }
        return null;
    }

    private List<SmartcardLibrary> getSmartcardLibraryList(String atrValue) throws SignerxException {
        SCXmlParser xmlParser = new SCXmlParser();
        List<SmartcardType> smartcardTypeList = xmlParser.readSmarcardDatabaseXml();
        for (SmartcardType smartcardType : smartcardTypeList) {
            List<SmartcardAtr> atrList = smartcardType.getAtrList();
            for (SmartcardAtr smartcardAtr : atrList) {
                if(smartcardAtr.getValue().equalsIgnoreCase(atrValue)) {
                    return smartcardType.getLibraryList();
                }
            }
        }
        LOGGER.error("ATR Degeri: " + atrValue + " - degeri icin kayit bulunamadi!");
        LOGGER.error("ATR degerini elle scdatabase.xml dosyasina ekleyebilirsiniz!");
        return null;
    }

    private EnumOsArch detectSystemArch() {
        String osArch = System.getProperty("os.arch");
        if(osArch.contains("64")) {
            return EnumOsArch.x64;
        }
        return EnumOsArch.x32;
    }

    private String detectLibraryExtension() throws SignerxException {
        String osName = System.getProperty("os.name");
        if (osName.contains(EnumOsName.Windows.name())) {
            return ".dll";
        } else if (osName.contains(EnumOsName.Linux.name())) {
            return ".so";
        } else if (osName.contains(EnumOsName.Mac.name())) {
            return ".dylib";
        } else {
            throw new SignerxException("Bilinmeyen isletim sistemi!");
        }
    }
}
