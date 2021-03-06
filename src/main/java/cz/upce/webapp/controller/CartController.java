package cz.upce.webapp.controller;

import cz.upce.webapp.controller.dto.PriceListDTO;
import cz.upce.webapp.controller.dto.SupplierItemsDTO;
import cz.upce.webapp.dao.stock.model.Supplier;
import cz.upce.webapp.dao.stock.repository.SupplierRepository;
import cz.upce.webapp.service.*;
import cz.upce.webapp.utils.xlsprocessors.ISheetProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author Tomas Kodym
 */

@Controller
public class CartController
{

    @Autowired private CartServiceImpl cartService;
    @Autowired private ItemServiceImpl itemService;
    @Autowired private SecurityServiceImpl securityService;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private EmailService emailService;
    @Autowired private ProcessorService processorService;

    @GetMapping("/cart")
    public String showCart(Model model)
    {
        Map<Integer, SupplierItemsDTO> itemsInCart = cartService.getItemsInCart();
        model.addAttribute("totalItemsCount", itemsInCart.size());
        model.addAttribute("itemsInCart", itemsInCart);
        model.addAttribute("message", cartService.getAndClearMessage());
        addUserToModel(model);

        return "cart/cart";
    }

    public String addUserToModel(Model model) {
        String userEmail = securityService.authenticatedUserEmail();
        model.addAttribute("user", userEmail);
        return userEmail;
    }

    @GetMapping("/cart/addItem/{itemId}")
    public String addToCart(@PathVariable Integer itemId, Model model)
    {
        itemService.findById(itemId).ifPresent(cartService::addItem);
        return showCart(model);
    }

    @GetMapping("/cart/removeItem/{itemId}")
    public String removeFromCart(@PathVariable Integer itemId, Model model)
    {
        itemService.findById(itemId).ifPresent(cartService::removeItem);
        return showCart(model);
    }

    @GetMapping("/order")
    public String order(@RequestParam Integer supplierId, Model model)
    {

        Supplier supplier = supplierRepository.getOne(supplierId);
        model.addAttribute(supplier);
        String message;
        if (addUserToModel(model).equals("info@pardubicebezobalu.cz")) {
            message = "Dobr?? den, v p????loze zas??l??m novou objedn??vku.\n" +
                    "\n" +
                    "Doru??te pros??m na n??sleduj??c?? adresu v Po-??t 15:00-17:30\n" +
                    "\n" +
                    "Kr??mek Bezobalu\n" +
                    "Nabersi s.r.o.\n" +
                    "Brozany 7\n" +
                    "53352 Star?? Hradi??t??\n" +
                    "\n" +
                    "I??O: 06758622\n" +
                    "Kontakt pro ??idi??e: 777045366\n" +
                    "\n" +
                    "D??kujeme v??m\n" +
                    "S pozdravem\n" +
                    "Pavel Jetensk??";
        } else {
            message = "Dobr?? den, v p????loze zas??l??m novou objedn??vku.\n" +
                    "\n" +
                    "Doru??te pros??m na n??sleduj??c?? adresu:\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "Kontakt pro ??idi??e: \n" +
                    "\n" +
                    "D??kujeme v??m\n" +
                    "S pozdravem\n" +
                    "";
        }

        model.addAttribute("message", message);
        return "cart/email";
    }

    @PostMapping("/order")
    public String sendOrderEmail(
            @RequestParam Integer supplierId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String cc,
            @RequestParam String subject,
            @RequestParam String body,
            Model model)
    {
        Supplier supplier = supplierRepository.getOne(supplierId);
        model.addAttribute(supplier);

        ISheetProcessor sheetProcessor = processorService.selectProcessor(supplierId);

        String message = null;
        try {
            PriceListDTO priceListDTO = processorService.getFilledPriceListWithOrder(supplierId);
            String attachmentFilename;
            message = emailService.sendMailWithAttachment(from, to, cc, subject, body, priceListDTO.getFilename(), priceListDTO.getContentType(), priceListDTO.getContent());
            cartService.removeSupplier(supplierId);
        } catch (IOException e) {
            e.printStackTrace();
            message = "Chyba p??i vytv????en?? vypln??n??ho cen??ku s objedn??vkou.";
        }
        cartService.setMessage(message);

        return "redirect:/cart";
    }
}
