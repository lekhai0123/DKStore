(function ($) {
    'use strict';

    // Preloader
    $(window).on('load', function () {
        $('#preloader').fadeOut('slow', function () {
            $(this).remove();
        });
    });

    // Instagram Feed
    if (($('#instafeed').length) !== 0) {
        var accessToken = $('#instafeed').attr('data-accessToken');
        var userFeed = new Instafeed({
            get: 'user',
            resolution: 'low_resolution',
            accessToken: accessToken,
            template: '<a href="{{link}}"><img src="{{image}}" alt="instagram-image"></a>'
        });
        userFeed.run();
    }

    setTimeout(function () {
        $('.instagram-slider').slick({
            dots: false,
            speed: 300,
            arrows: false,
            slidesToShow: 6,
            slidesToScroll: 1,
            responsive: [{
                    breakpoint: 1024,
                    settings: {
                        slidesToShow: 4
                    }
                },
                {
                    breakpoint: 600,
                    settings: {
                        slidesToShow: 3
                    }
                },
                {
                    breakpoint: 480,
                    settings: {
                        slidesToShow: 2
                    }
                }
            ]
        });
    }, 1500);

    // e-commerce touchspin
    $('input[name=\'soluong\']').TouchSpin();

    // Video Lightbox
    $(document).on('click', '[data-toggle="lightbox"]', function (event) {
        event.preventDefault();
        $(this).ekkoLightbox();
    });

    // Count Down JS
    $('#simple-timer').syotimer({
        year: 2022,
        month: 5,
        day: 9,
        hour: 20,
        minute: 30
    });

    // Hero Slider
    $('.hero-slider').slick({
        infinite: true,
        arrows: true,
        prevArrow: '<button type=\'button\' class=\'heroSliderArrow prevArrow tf-ion-chevron-left\'></button>',
        nextArrow: '<button type=\'button\' class=\'heroSliderArrow nextArrow tf-ion-chevron-right\'></button>',
        dots: true,
        autoplaySpeed: 7000,
        pauseOnFocus: false,
        pauseOnHover: false
    });
    $('.hero-slider').slickAnimation();

	$(document).ready(function() {
	       // Tạo phần tử <link>
	       var link = $('<link rel="shortcut icon" type="image/x-icon" />').attr('href', '/assets/img/logo3.png');
	       
	       // Thêm thẻ <link> vào trong thẻ <head>
	       $('head').append(link);
	   });
	   document.addEventListener("DOMContentLoaded", function() {
	   	    document.title = "DKStore";  // Tiêu đề mới mà bạn muốn thay thế
	   	});
})(jQuery);
