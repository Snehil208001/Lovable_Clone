import Image from "next/image";
import Link from "next/link";

interface LogoProps {
  size?: "sm" | "md" | "lg";
  showText?: boolean;
  className?: string;
  href?: string;
}

export function Logo({
  size = "md",
  showText = true,
  className = "",
  href,
}: LogoProps) {
  const iconDimensions = {
    sm: { width: 28, height: 28, container: "size-7" },
    md: { width: 36, height: 36, container: "size-9" },
    lg: { width: 48, height: 48, container: "size-12" },
  }[size];

  const textSizes = {
    sm: "text-sm",
    md: "text-base",
    lg: "text-xl",
  }[size];

  const content = (
    <div className={`flex items-center gap-2.5 ${className}`}>
      <div className={`relative overflow-hidden rounded-xl border border-primary/30 bg-primary/10 shadow-glow-primary flex items-center justify-center ${iconDimensions.container}`}>
        <Image
          src="/logo.png"
          alt="AuraCode Logo"
          width={iconDimensions.width}
          height={iconDimensions.height}
          className="object-cover rounded-xl"
          priority
        />
      </div>
      {showText && (
        <div className="flex flex-col justify-center">
          <span className={`font-heading font-bold tracking-tight bg-gradient-to-r from-foreground via-foreground to-muted-foreground bg-clip-text text-transparent ${textSizes}`}>
            AuraCode
          </span>
        </div>
      )}
    </div>
  );

  if (href) {
    return (
      <Link href={href} className="inline-block transition-opacity hover:opacity-90">
        {content}
      </Link>
    );
  }

  return content;
}
