package com.fastbills.api.hsn;

import com.fastbills.api.hsn.dto.HsnLookupResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class HsnValidationService {

    private static final Map<String, HsnMasterEntry> GST_MASTER = new HashMap<>();

    record HsnMasterEntry(String code, String name, String description, BigDecimal rate, boolean isService) {}

    static {
        // SAC Services (Chapter 99)
        add("998311", "Management Consulting Services", "Management consulting and management services including financial, strategic and HR consulting", new BigDecimal("18.00"), true);
        add("998312", "Business Consulting Services", "Business consulting services including public relations and business development", new BigDecimal("18.00"), true);
        add("998313", "Information Technology Software Services", "Information technology software design, development, customization, and implementation services", new BigDecimal("18.00"), true);
        add("998314", "IT Infrastructure and Network Services", "Information technology infrastructure provisioning, maintenance, and network management services", new BigDecimal("18.00"), true);
        add("998315", "Hosting and Cloud Infrastructure Services", "Website hosting, cloud data storage, application service provisioning, and infrastructure services", new BigDecimal("18.00"), true);
        add("998316", "IT Support and Maintenance Services", "Computer systems maintenance, technical software support, and disaster recovery services", new BigDecimal("18.00"), true);
        add("998319", "Other Information Technology Services", "Other information technology and computer related services nowhere else classified", new BigDecimal("18.00"), true);
        add("998211", "Legal Advisory and Representation Services", "Legal advisory, representation, and documentation preparation services", new BigDecimal("18.00"), true);
        add("998221", "Financial Auditing Services", "Financial auditing, examination of accounting records, and financial statement review services", new BigDecimal("18.00"), true);
        add("998222", "Accounting and Bookkeeping Services", "Accounting, bookkeeping, payroll compilation, and tax preparation services", new BigDecimal("18.00"), true);
        add("998361", "Advertising and Creative Services", "Advertising concepts, creative copywriting, artwork development, and ad placement services", new BigDecimal("18.00"), true);
        add("998371", "Market Research and Polling Services", "Market research, consumer preference surveys, and public opinion polling services", new BigDecimal("18.00"), true);
        add("998411", "Telecommunication Services", "Fixed telephony, mobile network, and broadband internet access services", new BigDecimal("18.00"), true);
        add("995411", "General Construction Services", "Construction of residential and commercial buildings, civil engineering projects", new BigDecimal("18.00"), true);
        add("996511", "Road Freight Transport Services", "Transportation of goods over land by motor vehicles and trucks", new BigDecimal("5.00"), true);
        add("997212", "Real Estate Rental Services", "Renting or leasing of commercial and industrial non-residential properties", new BigDecimal("18.00"), true);
        add("998711", "Maintenance and Repair of Machinery", "Maintenance, servicing, and repair of industrial machinery and mechanical equipment", new BigDecimal("18.00"), true);
        add("998713", "Maintenance and Repair of Computers", "Repair and maintenance services of computers, laptops, and peripheral office equipment", new BigDecimal("18.00"), true);
        add("999210", "Higher Education and Training Services", "Post-secondary and commercial training, vocational skill development services", new BigDecimal("18.00"), true);

        // Standard Goods (HSN Chapters)
        add("84713010", "Personal Computers and Laptops", "Portable automatic data processing machines, weighing not more than 10 kg, laptop computers", new BigDecimal("18.00"), false);
        add("8471", "Automatic Data Processing Machines", "Computers, computing units, magnetic/optical readers, and computer hardware accessories", new BigDecimal("18.00"), false);
        add("8517", "Telephone Sets and Smartphones", "Smartphones, cellular telephones, and apparatus for transmission/reception of voice or data", new BigDecimal("18.00"), false);
        add("8528", "Monitors and Projectors", "Monitors, display screens, video projectors not incorporating television reception apparatus", new BigDecimal("18.00"), false);
        add("8504", "Electrical Transformers and Power Adapters", "Static converters, power supply units, UPS, and battery chargers for computing equipment", new BigDecimal("18.00"), false);
        add("8443", "Printers and Multifunction Copiers", "Printers, copying machines, facsimile machines, whether or not combined", new BigDecimal("18.00"), false);
        add("4901", "Printed Books and Brochures", "Printed books, brochures, leaflets, and similar printed matter", new BigDecimal("0.00"), false);
        add("4911", "Other Printed Matter and Trade Advertising", "Trade advertising material, commercial catalogues, brochures, and marketing posters", new BigDecimal("18.00"), false);
        add("4820", "Paper Stationery and Registers", "Registers, account books, note books, order books, receipt books, and office stationery", new BigDecimal("18.00"), false);
        add("9403", "Office and Wooden Furniture", "Office desks, ergonomic chairs, metal and wooden furniture used in offices", new BigDecimal("18.00"), false);
        add("3004", "Medicaments and Pharmaceutical Formulations", "Medicaments consisting of mixed or unmixed products for therapeutic or prophylactic uses", new BigDecimal("12.00"), false);
        add("6109", "T-shirts and Vests of Cotton", "T-shirts, singlets, and other vests, knitted or crocheted, of cotton or synthetic fibres", new BigDecimal("5.00"), false);
        add("6203", "Men's or Boys' Suits and Trousers", "Men's or boys' suits, ensembles, jackets, blazers, trousers, bib and brace overalls", new BigDecimal("12.00"), false);
        add("2106", "Food Preparations and Nutritional Supplements", "Food preparations not elsewhere specified or included, dietary health supplements", new BigDecimal("18.00"), false);
        add("1905", "Bread, Pastry, Cakes and Biscuits", "Bread, pastries, cakes, biscuits, and other bakers' wares", new BigDecimal("18.00"), false);
        add("2202", "Waters and Aerated Beverages", "Waters including mineral and aerated waters containing added sugar, and flavored drinks", new BigDecimal("28.00"), false);
    }

    private static void add(String code, String name, String desc, BigDecimal rate, boolean isService) {
        GST_MASTER.put(code, new HsnMasterEntry(code, name, desc, rate, isService));
    }

    public HsnLookupResponse lookup(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("HSN/SAC code cannot be empty");
        }

        String cleanCode = code.trim().replaceAll("[^0-9]", "");
        if (cleanCode.length() < 2 || cleanCode.length() > 8) {
            return new HsnLookupResponse(code, "Unknown HSN/SAC", "Invalid code length: must be 2 to 8 numeric digits", new BigDecimal("18.00"), false, false);
        }

        // 1. Direct match
        if (GST_MASTER.containsKey(cleanCode)) {
            HsnMasterEntry entry = GST_MASTER.get(cleanCode);
            return new HsnLookupResponse(entry.code(), entry.name(), entry.description(), entry.rate(), entry.isService(), true);
        }

        // 2. Prefix matching (e.g. 847130 matches 8471)
        for (int len = cleanCode.length() - 1; len >= 2; len--) {
            String prefix = cleanCode.substring(0, len);
            if (GST_MASTER.containsKey(prefix)) {
                HsnMasterEntry entry = GST_MASTER.get(prefix);
                return new HsnLookupResponse(
                        cleanCode,
                        entry.name() + " (" + cleanCode + ")",
                        entry.description() + " [Derived from Heading " + prefix + "]",
                        entry.rate(),
                        entry.isService(),
                        true
                );
            }
        }

        // 3. Chapter-level classification
        boolean isService = cleanCode.startsWith("99");
        String chapter = cleanCode.substring(0, 2);
        String chapterName = getChapterDescription(chapter, isService);

        return new HsnLookupResponse(
                cleanCode,
                chapterName,
                (isService ? "Service Accounting Code (SAC) Chapter 99" : "Harmonized System of Nomenclature (HSN) Chapter " + chapter) + " - Classified under Indian GST Tariff",
                new BigDecimal("18.00"),
                isService,
                true
        );
    }

    private String getChapterDescription(String chapter, boolean isService) {
        if (isService) {
            return "Services under SAC 99";
        }
        return switch (chapter) {
            case "01", "02", "03", "04", "05" -> "Animal and Dairy Products (Chapter " + chapter + ")";
            case "06", "07", "08", "09", "10", "11", "12", "13", "14" -> "Vegetable and Agricultural Products (Chapter " + chapter + ")";
            case "15" -> "Animal or Vegetable Fats and Oils";
            case "16", "17", "18", "19", "20", "21", "22", "23", "24" -> "Prepared Foodstuffs, Beverages and Tobacco (Chapter " + chapter + ")";
            case "25", "26", "27" -> "Mineral Products and Fuels (Chapter " + chapter + ")";
            case "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38" -> "Chemical and Pharmaceutical Products (Chapter " + chapter + ")";
            case "39", "40" -> "Plastics and Rubber Articles (Chapter " + chapter + ")";
            case "41", "42", "43" -> "Raw Hides, Skins and Leather Goods (Chapter " + chapter + ")";
            case "44", "45", "46" -> "Wood and Wood Articles (Chapter " + chapter + ")";
            case "47", "48", "49" -> "Paper, Pulp and Printed Matter (Chapter " + chapter + ")";
            case "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63" -> "Textiles and Textile Articles (Chapter " + chapter + ")";
            case "64", "65", "66", "67" -> "Footwear, Headgear and Umbrellas (Chapter " + chapter + ")";
            case "68", "69", "70" -> "Articles of Stone, Plaster, Cement, Ceramic and Glass";
            case "71" -> "Natural/Cultured Pearls, Precious Stones and Jewellery";
            case "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83" -> "Base Metals and Articles of Base Metal (Chapter " + chapter + ")";
            case "84", "85" -> "Machinery, Electrical Equipment and Electronics (Chapter " + chapter + ")";
            case "86", "87", "88", "89" -> "Vehicles, Aircraft, Vessels and Transport Equipment (Chapter " + chapter + ")";
            case "90", "91", "92" -> "Optical, Photographic, Measuring and Medical Instruments";
            case "94", "95", "96" -> "Furniture, Toys, Games and Miscellaneous Manufactured Articles";
            default -> "Goods under HSN Chapter " + chapter;
        };
    }
}
